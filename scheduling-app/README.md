# scheduling-app

Сервис управления расписанием: игровые слоты, зоны отдыха/переодевания и их бронирование.

## Быстрый старт

```bash
# 1. Поднять PostgreSQL
docker compose up -d

# 2. Запустить сервис (порт 8081)
mvn -pl scheduling-app spring-boot:run

# 3. Swagger UI
open http://localhost:8081/swagger-ui
```

**Требования:** Java 21, PostgreSQL 16 (порт 5433, `pb/pb`), база `pb_scheduling`.

## Конфигурация

`src/main/resources/application.yml`

| Свойство | Значение | Описание |
|----------|----------|----------|
| `server.port` | `8081` | HTTP-порт |
| `pb.scheduling.hold-minutes` | `15` | TTL для HOLD-резервации (мин) |
| `pb.scheduling.default-duration-minutes_for_zones` | `180` | Длительность резервации зоны от начала слота (мин) |
| `pb.scheduling.hold-cleanup-interval-ms` | `60000` | Интервал фоновой очистки просроченных HOLD (мс) |

Свойства биндятся через `SchedulingProperties` record (`@ConfigurationProperties(prefix = "pb.scheduling")`).

## Доменная модель

```
┌──────────┐       ┌────────────┐        ┌──────────────────┐
│  Arena   │──1:N──│  GameSlot  │ ──1:N──│ SlotReservation  │
└──────────┘       └────────────┘        └──────────────────┘
                                               │
                                               │ slotReservationId
                                               ▼
┌──────────┐                            ┌──────────────────┐
│   Zone   │───────────1:N──────────────│ ZoneReservation  │
└──────────┘                            └──────────────────┘
                                               │ parentReservationId
                                               ▼ (self-ref для цепочки продлений)
                                        ┌──────────────────┐
                                        │ ZoneReservation  │
                                        └──────────────────┘
```

### Сущности

#### Arena
Справочник физических площадок.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | PK |
| `name` | String | Название |
| `active` | boolean | Активна ли (default: true) |

#### GameSlot
Временной интервал на арене с ограниченной вместимостью.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | PK |
| `arenaId` | UUID | FK → Arena |
| `startTime` | OffsetDateTime | Начало |
| `endTime` | OffsetDateTime | Конец |
| `capacityCompanies` | int | Макс. количество компаний |
| `status` | GameSlotStatus | `OPEN` / `CLOSED` / `CANCELLED` |

#### SlotReservation
Бронь места в GameSlot.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | PK |
| `gameSlotId` | UUID | FK → GameSlot |
| `bookingId` | UUID | Внешний ID бронирования (из booking-app) |
| `status` | SlotReservationStatus | `HOLD` / `CONFIRMED` / `CANCELLED` |
| `createdAt` | OffsetDateTime | Время создания |
| `expiresAt` | OffsetDateTime | Срок истечения HOLD (null для CONFIRMED) |
| `exclusive` | boolean | Эксклюзивное бронирование (default: false) |

**DB constraint:** если `status = HOLD`, то `expires_at IS NOT NULL`.

#### Zone
Справочник зон (отдых/переодевание).

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | PK |
| `code` | String | Уникальный код |
| `name` | String | Название |
| `type` | ZoneType | `REST` / `DRESSING` |
| `capacityCompanies` | int | Макс. количество компаний |
| `paid` | boolean | Платная (default: true) |
| `active` | boolean | Активна (default: true) |

#### ZoneReservation
Временная резервация зоны с поддержкой цепочки продлений.

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | UUID | PK |
| `zoneId` | UUID | FK → Zone |
| `bookingId` | UUID | Внешний ID бронирования |
| `slotReservationId` | UUID | FK → SlotReservation |
| `startTime` | OffsetDateTime | Начало |
| `endTime` | OffsetDateTime | Конец |
| `status` | ZoneReservationStatus | `HOLD` / `ACTIVE` / `CANCELLED` / `FINISHED` |
| `createdAt` | OffsetDateTime | Время создания |
| `expiresAt` | OffsetDateTime | Срок истечения HOLD |
| `parentReservationId` | UUID | FK → ZoneReservation (для продлений, null у базовой) |

## Машины состояний

### SlotReservation

```
  ┌──────┐    confirm    ┌───────────┐
  │ HOLD │──────────────▶│ CONFIRMED │
  └──┬───┘               └─────┬─────┘
     │ cancel / expire          │ cancel
     ▼                          ▼
  ┌───────────┐◀────────────────┘
  │ CANCELLED │
  └───────────┘
```

- **HOLD** создается с `expiresAt = now + holdMinutes`
- **confirm** проверяет, что `expiresAt > now`
- **cancel** применим к `HOLD` и `CONFIRMED`; идемпотентен (повторный вызов на `CANCELLED` — no-op)
- **expire** — фоновая задача (`HoldCleanupJob`) каждые 60 сек отменяет просроченные HOLD

### ZoneReservation

```
  ┌──────┐    confirm    ┌────────┐   finish   ┌──────────┐
  │ HOLD │──────────────▶│ ACTIVE │────────────▶│ FINISHED │
  └──┬───┘               └───┬──┬─┘             └──────────┘
     │ cancel / expire       │  │ extend / cancel
     ▼                       │  ▼
  ┌───────────┐◀─────────────┘ (новая запись
  │ CANCELLED │                 ACTIVE с
  └───────────┘                 parentReservationId)
```

- **HOLD → ACTIVE** при confirm, повторно проверяются overlaps (race condition protection)
- **cancel** применим к `HOLD` и `ACTIVE`; идемпотентен (повторный вызов на `CANCELLED` — no-op)
- **extend** не обновляет существующую запись, а создает **новую** `ZoneReservation` со статусом `ACTIVE` и `parentReservationId` = id базовой
- **finish-chain** помечает все записи с совпадающим `(bookingId, zoneId)` в статусе `ACTIVE` как `FINISHED`

## API

### Слоты (SlotController)

| Метод | Путь | Описание | Код |
|-------|------|----------|-----|
| `GET` | `/slots/getAll` | Все игровые слоты | 200 |
| `GET` | `/slot-resrv/getAll` | Все резервации слотов | 200 |
| `POST` | `/slots/{slotId}/holds` | Создать HOLD на слот | 201 |
| `POST` | `/slot-holds/{id}/confirm` | Подтвердить HOLD | 204 |
| `POST` | `/slot-holds/{id}/cancel` | Отменить HOLD | 204 |

#### POST /slots/{slotId}/holds

Запрос:
```json
{
  "bookingId": "uuid",
  "exclusive": false
}
```

Ответ (201):
```json
{
  "slotReservationId": "uuid",
  "expiresAt": "2025-01-01T12:15:00Z"
}
```

#### POST /slot-holds/{id}/confirm, /cancel

Запрос:
```json
{
  "bookingId": "uuid"
}
```
Ответ: 204 No Content

### Зоны — CRUD (ZoneController)

| Метод | Путь | Описание | Код |
|-------|------|----------|-----|
| `POST` | `/zones` | Создать зону | 201 |
| `GET` | `/zones` | Список зон (опц. `?active=true`) | 200 |
| `GET` | `/zones/{id}` | Зона по ID | 200 |
| `PATCH` | `/zones/{id}` | Обновить зону (partial update) | 200 |
| `DELETE` | `/zones/{id}` | Деактивировать зону (soft delete) | 204 |

#### POST /zones

```json
{
  "code": "REST1",
  "name": "Зона отдыха 1",
  "type": "REST",
  "capacityCompanies": 3,
  "paid": true
}
```

### Резервации зон

| Метод | Путь | Описание | Код |
|-------|------|----------|-----|
| `GET` | `/zone-reservations` | Список (фильтры: `zoneId`, `bookingId`, `status`) | 200 |
| `GET` | `/zone-reservations/{id}` | По ID | 200 |
| `POST` | `/zones/{zoneId}/holds` | Создать HOLD на зону | 201 |
| `POST` | `/zone-holds/{id}/confirm` | Подтвердить | 204 |
| `POST` | `/zone-holds/{id}/cancel` | Отменить | 204 |
| `POST` | `/zone-reservations/{id}/extend` | Продлить (создает новую запись) | 201 |
| `POST` | `/zone-reservations/{id}/finish-chain` | Завершить всю цепочку | 204 |

#### POST /zones/{zoneId}/holds

```json
{
  "bookingId": "uuid",
  "slotReservationId": "uuid"
}
```

Ответ (201):
```json
{
  "zoneReservationId": "uuid",
  "expiresAt": "2025-01-01T12:15:00Z"
}
```

#### POST /zone-reservations/{id}/extend

```json
{
  "bookingId": "uuid",
  "extendMinutes": 30
}
```

Ответ (201): UUID новой записи продления

## Паттерн конкурентного доступа

Все операции, связанные с проверкой вместимости, используют **пессимистическую блокировку** (`PESSIMISTIC_WRITE`):

1. Блокировка строки ресурса через `findByIdForUpdate`
2. Подсчет активных резерваций: `CONFIRMED` + не истекшие `HOLD` (где `expiresAt > now`)
3. Сравнение с `capacityCompanies`
4. Мутация, если есть свободное место

Zone confirm **повторно проверяет overlaps** после блокировки (защита от race condition между hold и confirm).

## Коды ошибок

Все ошибки возвращаются в формате:
```json
{
  "code": "SLOT_FULL",
  "message": "Описание ошибки",
  "details": {}
}
```

### Слоты

| Код | HTTP | Описание |
|-----|------|----------|
| `SLOT_NOT_FOUND` | 404 | Слот не найден |
| `SLOT_NOT_OPEN` | 400 | Слот не в статусе OPEN |
| `SLOT_FULL` | 400 | Нет свободных мест |
| `SLOT_EXCLUSIVE` | 400 | Конфликт эксклюзивности |
| `SLOT_HOLD_NOT_FOUND` | 404 | Резервация слота не найдена |
| `INVALID_STATUS` | 400 | Недопустимый статус для операции |
| `HOLD_EXPIRED` | 400 | HOLD просрочен |

### Зоны

| Код | HTTP | Описание |
|-----|------|----------|
| `ZONE_NOT_FOUND` | 404 | Зона не найдена |
| `ZONE_CODE_DUPLICATE` | 400 | Код зоны уже существует |
| `ZONE_INACTIVE` | 400 | Зона деактивирована |
| `ZONE_BUSY` | 400 | Нет свободных мест в зоне на данный интервал |
| `ZONE_RES_NOT_FOUND` | 404 | Резервация зоны не найдена |
| `ZONE_HOLD_NOT_FOUND` | 404 | HOLD зоны не найден |
| `SLOT_RES_NOT_FOUND` | 404 | Резервация слота не найдена (при создании hold зоны) |
| `BOOKING_MISMATCH` | 400 | bookingId не совпадает с резервацией |
| `SLOT_RES_STATUS_MISMATCH` | 400 | Резервация слота не в HOLD/CONFIRMED |
| `SLOT_HOLD_EXPIRED` | 400 | HOLD слота просрочен |
| `SLOT_ALREADY_FINISHED` | 400 | Игровой слот уже завершен |
| `GAME_SLOT_NOT_FOUND` | 404 | GameSlot не найден |
| `INVALID_TIME` | 400 | Некорректный временной интервал продления |
| `INVALID_REQUEST` | 400 | Отсутствует обязательный параметр |

## Фоновые задачи

### HoldCleanupJob

Запускается каждые `hold-cleanup-interval-ms` (по умолчанию 60 сек). Находит все HOLD с `expiresAt < now` и переводит в `CANCELLED`. Работает для обеих таблиц: `slot_reservation` и `zone_reservation`.

## Структура пакетов

```
com.pb.scheduling
├── SchedulingApplication.java
├── api
│   ├── advice
│   │   └── GlobalExceptionHandler.java
│   ├── controller
│   │   ├── SlotController.java
│   │   └── ZoneController.java
│   └── dto
│       ├── request
│       │   ├── ConfirmByBookingRequest.java
│       │   ├── CreateZoneRequest.java
│       │   ├── ExtendZoneRequest.java
│       │   ├── HoldSlotRequest.java
│       │   ├── HoldZoneRequest.java
│       │   └── UpdateZoneRequest.java
│       └── response
│           ├── HoldSlotResponse.java
│           ├── HoldZoneResponse.java
│           ├── ZoneReservationResponse.java
│           └── ZoneResponse.java
├── config
│   └── SchedulingProperties.java
├── domain
│   └── entity
│       ├── Arena.java
│       ├── GameSlot.java
│       ├── SlotReservation.java
│       ├── Zone.java
│       └── ZoneReservation.java
├── exception
│   ├── ApiError.java
│   ├── BusinessException.java
│   └── NotFoundException.java
├── repository
│   ├── GameSlotRepository.java
│   ├── SlotReservationRepository.java
│   ├── ZoneRepository.java
│   └── ZoneReservationRepository.java
└── service
    ├── SlotService.java
    ├── ZoneService.java
    └── jobs
        └── HoldCleanupJob.java
```

## База данных

- **Схема:** `scheduling`
- **Миграции:** Flyway, `src/main/resources/db/migration/` (V0–V8)
- **Все timestamps:** `TIMESTAMPTZ`, Hibernate timezone: `UTC`
- **DDL:** `validate` — Flyway создает схему, Hibernate только проверяет

### Seed data (из миграций)

- 3 арены (Arena1, Arena2, Arena3)
- 4 зоны (REST1, REST2, REST3 — тип REST; DRESSING — тип DRESSING)
- Игровые слоты на 7 дней вперед: 4 интервала/день (09–12, 12–15, 15–18, 18–21 MSK), 3 арены, вместимость = 2

## Тестирование

```bash
# Все тесты
mvn -pl scheduling-app test

# Конкретный класс
mvn -pl scheduling-app test -Dtest=SlotServiceTest

# Конкретный метод
mvn -pl scheduling-app test -Dtest=SlotServiceTest#holdSlot_whenSlotFull_throwsException
```

Тесты используют **Mockito** (без Spring context, без БД). `SchedulingProperties` создается напрямую: `new SchedulingProperties(15, 180)`.

### Покрытие

| Класс | Тестов | Что покрыто |
|-------|--------|-------------|
| `SlotServiceTest` | 7 | hold (success, full, exclusive), confirm (expired), cancel (idempotent) |
| `ZoneServiceTest` | 4 | hold (success, busy), extend (invalid status, success) |

## Межсервисное взаимодействие

Scheduling-app — **поставщик** для booking-app. Booking-app вызывает scheduling-app через WebFlux `SchedulingClient`. `bookingId` в резервациях — логический FK, не физический. Общих БД нет.

**Порядок запуска:** auth-service → scheduling-app → booking-app

---

## Аудит кода: известные проблемы и рекомендации

> Состояние на 2026-03-26. Раздел описывает найденные баги, пробелы в тестах и рекомендации по улучшению.
> Пофикшенные баги отмечены ✅.

### Баги

#### ✅ BUG-1. `SlotService.confirmHold()` и `cancelHold()` — нет пессимистической блокировки

**Статус:** исправлено

Добавлен метод `findByIdAndBookingIdForUpdate()` с `@Lock(PESSIMISTIC_WRITE)` в `SlotReservationRepository`. `confirmHold()` и `cancelHold()` теперь используют его вместо `findByIdAndBookingId()`.

---

#### ✅ BUG-2. `SlotService.cancelHold()` — не очищает `expiresAt`

**Статус:** исправлено

Добавлен `r.setExpiresAt(null)` при переводе в `CANCELLED`. Поведение приведено в соответствие с `ZoneService.cancelZoneHold()`.

---

#### ✅ BUG-3. `ZoneService.finishChain()` — нет блокировки

**Статус:** исправлено

`findById()` заменён на `findByIdForUpdate()`. Race condition с параллельным `extend()` устранён.

---

#### ✅ BUG-4. `finishChain()` — нарушение машины состояний: `HOLD → FINISHED`

**Статус:** исправлено

Убрано условие `HOLD` из цикла в `finishChain()`. Теперь только `ACTIVE → FINISHED`. Просроченные HOLDы остаются для `HoldCleanupJob` (`HOLD → CANCELLED`).

---

#### ✅ BUG-5. `GlobalExceptionHandler` — возвращает 400 вместо 422

**Статус:** исправлено

`HttpStatus.BAD_REQUEST` заменён на `HttpStatus.UNPROCESSABLE_ENTITY` для `BusinessException`. `NotFoundException` по-прежнему возвращает 404.

---

#### BUG-6. ~~`cancelHold()` позволяет отменить CONFIRMED-резервацию~~ — намеренное поведение

**Статус:** не баг — бизнес-решение

Переход `CONFIRMED → CANCELLED` разрешён: администратор может отменить ошибочно подтверждённую резервацию. Машина состояний обновлена в разделе выше.

---

#### ✅ BUG-7. `holdSlot()` — нет проверки, что слот не в прошлом

**Статус:** исправлено

Добавлена проверка `slot.getEndTime().isAfter(now)` перед созданием HOLD. Бросает `SLOT_ALREADY_FINISHED` (422). Поведение приведено в соответствие с `ZoneService.holdZone()`.

---

#### ✅ BUG-8. `OffsetDateTime.now()` без явного часового пояса

**Статус:** исправлено

Реализован `Clock`-бин (`AppConfig.java`) с часовым поясом `Europe/Moscow`, настраиваемым через `pb.scheduling.timezone` в `application.yml`. `Clock` инжектируется в `SlotService`, `ZoneService` и `HoldCleanupJob`. Hibernate timezone также переключён на `Europe/Moscow` — API возвращает даты с `+03:00`.

---

### Проблемы безопасности

| # | Проблема | Файл | Приоритет |
|---|----------|------|-----------|
| SEC-1 | `include-stacktrace: always` — полный стектрейс утекает клиенту | `application.yml:6` | ВЫСОКИЙ |
| SEC-2 | `bookingId` берется из тела запроса, а не из JWT-токена — можно отменить/подтвердить чужую резервацию | `SlotController`, `ZoneController` | ВЫСОКИЙ |
| SEC-3 | Нет аутентификации — все эндпоинты публично доступны (нет Spring Security, `@PreAuthorize`, JWT-фильтров) | Все контроллеры | КРИТИЧЕСКИЙ |

---

### Мертвый код

| Файл | Описание |
|------|----------|
| `SlotReservationRepository.java` | `findSimple()` — не используется, дублирует `findById()` |
| `GlobalExceptionHandler.java` | Закомментированный `handleOther()` — дубликат `handleAny()` |

---

### Архитектурные замечания

- **`updateZone()` без блокировки** (`ZoneService.java`): использует `findById()` вместо `findByIdForUpdate()`. Уменьшение `capacityCompanies` одновременно с созданием HOLD — race condition.
- **Нет валидации конфигурации** (`SchedulingProperties`): `holdMinutes` и `defaultDurationMinutesForZones` могут быть 0 или отрицательными. Код использует костыль `Math.max(1, ...)`. Нужно `@Min(1)` на record-компонентах.
- **`confirmHold` не идемпотентен** — повторный вызов на CONFIRMED-резервацию бросает `INVALID_STATUS`. `cancelHold` при этом идемпотентен. Несогласованность API-контракта.

---

### Пробелы в тестах

#### Методы без тестов

**SlotService** (2 из 5):
- `getAllGameSlots()`
- `getAllSlotReservations()`

**ZoneService** (10 из 14):
- `createZone()`, `getAllZones()`, `getZone()`, `updateZone()`, `deactivateZone()`
- `getZoneReservation()`, `listZoneReservations()`
- `confirmZoneHold()`, `cancelZoneHold()`, `finishChain()`

#### Непокрытые ветки в тестируемых методах

| Метод | Что не покрыто |
|-------|----------------|
| `holdSlot` | `SLOT_NOT_FOUND`, `SLOT_NOT_OPEN`, `SLOT_ALREADY_FINISHED` |
| `confirmHold` | success path, `SLOT_HOLD_NOT_FOUND`, повторный confirm |
| `cancelHold` | отмена HOLD (не CANCELLED), `SLOT_HOLD_NOT_FOUND` |
| `holdZone` | `ZONE_INACTIVE`, `BOOKING_MISMATCH`, `SLOT_HOLD_EXPIRED`, `GAME_SLOT_NOT_FOUND`, `SLOT_ALREADY_FINISHED` |
| `extend` | `BOOKING_MISMATCH`, `ZONE_BUSY`, `INVALID_TIME` |

#### Отсутствующие категории тестов

- **Контроллеры** (`@WebMvcTest`) — HTTP-статусы, валидация `@Valid`, сериализация
- **Репозитории** (`@DataJpaTest`) — особенно `countOverlaps` и `countActive` (сложные JPQL-запросы)
- **Интеграционные** — полные workflows hold → confirm → extend → finish
- **Конкурентные** — проверка пессимистических блокировок под нагрузкой

---

### Рекомендации по улучшению

#### Приоритет 1 — Исправить до продакшена

1. `include-stacktrace: never` для production-профиля (SEC-1)
2. Добавить Spring Security / JWT-фильтр (SEC-3)

#### Приоритет 2 — Улучшения качества

3. Валидация `SchedulingProperties` с `@Validated` и `@Min(1)`
4. Удалить мертвый код (`findSimple`, комментарии в GlobalExceptionHandler)
5. Написать тесты для `confirmZoneHold`, `cancelZoneHold`, `finishChain` — критические бизнес-операции без единого теста

#### Приоритет 3 — Хорошие практики

6. Добавить `@DataJpaTest` для `countOverlaps` — сложный запрос, легко сломать
7. Добавить верхнюю границу для `capacityCompanies` (`@Max`)
8. Health check эндпоинт для Docker/K8s readiness probe
9. Метрики (Micrometer) — количество hold/confirm/cancel, время ответов, процент expired holds
