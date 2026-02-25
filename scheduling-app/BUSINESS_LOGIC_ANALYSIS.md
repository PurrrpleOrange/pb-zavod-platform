# Scheduling Service — полный разбор бизнес-логики (по фактическому коду)

Документ описывает **реально реализованную** бизнес-логику модуля `scheduling-app` в текущем состоянии репозитория.

---

## 1. Назначение сервиса и границы ответственности

`scheduling-app` — сервис, который управляет занятостью ресурсов по времени:

- игровых слотов (`game_slot`) на аренах;
- мест внутри слота для компаний (`slot_reservation`);
- зон (`zone`) и их временных резервов (`zone_reservation`).

В текущей реализации сервис покрывает прежде всего **операционный контур бронирования**:

- поставить HOLD на слот;
- подтвердить HOLD слота;
- отменить HOLD слота;
- поставить HOLD на зону (привязанную к уже созданной резервации слота);
- подтвердить/отменить HOLD зоны;
- продлить активную резервацию зоны.

В коде есть entity `Arena`, но публичных endpoint’ов для CRUD арен/слотов/зон в стиле админ-панели пока нет.

---

## 2. Архитектура и слои

Сервис построен как типичный Spring Boot модуль:

- **API слой** (`api/controller`, `api/dto`) — HTTP контракты;
- **Service слой** (`service`) — бизнес-правила и транзакционные сценарии;
- **Repository слой** (`repository`) — JPA доступ к БД, включая блокировки и агрегаты (`count...`);
- **Domain слой** (`domain/entity`, `domain/enums`) — модель данных;
- **Error handling** (`api/advice`, `exception`) — единообразные бизнес-ошибки.

Ключевой принцип: критичные операции HOLD/CONFIRM/EXTEND идут внутри `@Transactional`, а в репозиториях применяются `PESSIMISTIC_WRITE`-блокировки для защиты от гонок.

---

## 3. Доменная модель и семантика

### 3.1 Arena

Сущность `Arena` содержит:

- `arena_id`;
- `name`;
- `is_active`.

Роль: справочник физических площадок.

### 3.2 GameSlot

`GameSlot` = временной интервал на арене:

- `game_slot_id`;
- `arena_id`;
- `start_time` / `end_time`;
- `capacity_companies`;
- `status`: `OPEN | CLOSED | CANCELLED`.

Смысл:

- `OPEN` — можно резервировать;
- `CLOSED` — резервировать нельзя;
- `CANCELLED` — слот отменён.

### 3.3 SlotReservation

`SlotReservation` = занятость 1 места в слоте конкретной бронью:

- `slot_reservation_id`;
- `game_slot_id`;
- `booking_id`;
- `status`: `HOLD | CONFIRMED | CANCELLED`;
- `created_at`, `expires_at`.

Активная нагрузка для capacity считается как:

- все `CONFIRMED`;
- + `HOLD`, у которых `expires_at > now`.

### 3.4 Zone

`Zone` = справочник зон:

- `zone_id`, `code`, `name`;
- `type`: `REST | DRESSING`;
- `capacity_companies`;
- `is_paid`, `is_active`.

### 3.5 ZoneReservation

`ZoneReservation` = интервал бронирования зоны:

- `zone_reservation_id`;
- `zone_id`, `booking_id`;
- `start_time`, `end_time`;
- `status`: `HOLD | ACTIVE | CANCELLED | FINISHED`;
- `created_at`, `expires_at`;
- `parent_reservation_id` (цепочка продлений);
- `slot_reservation_id` (связь с резервацией слота).

Для конфликтов учитываются:

- `ACTIVE` всегда;
- `HOLD`, если не истёк (`expires_at > now`).

---

## 4. Статусы и жизненные циклы

## 4.1 Жизненный цикл slot reservation

Базовый flow:

1. `POST /slots/{slotId}/holds` → создаётся `HOLD` с `expiresAt`.
2. `POST /slot-holds/{id}/confirm` + `bookingId` → `HOLD -> CONFIRMED`.
3. `POST /slot-holds/{id}/cancel` + `bookingId` → `... -> CANCELLED`.

Ограничения:

- confirm допустим только из `HOLD`;
- confirm невозможен после истечения hold;
- cancel идемпотентен для уже `CANCELLED`.

## 4.2 Жизненный цикл zone reservation

1. `POST /zones/{zoneId}/holds` → `HOLD` зоны.
2. `POST /zone-holds/{id}/confirm` → `HOLD -> ACTIVE`.
3. `POST /zone-holds/{id}/cancel` → `... -> CANCELLED`.
4. `POST /zone-reservations/{id}/extend` → создаётся **новая** `ACTIVE` запись-продление.

`FINISHED` присутствует в enum, но в сервисе не выставляется.

---

## 5. API и фактические контракты

## 5.1 Слоты

- `GET /slots/getAll` — вернуть все `GameSlot`.
- `GET /slot-resrv/getAll` — вернуть все `SlotReservation`.
- `POST /slots/{slotId}/holds` — HOLD слота (`bookingId` в body).
- `POST /slot-holds/{slotReservationId}/confirm` — confirm HOLD по `bookingId`.
- `POST /slot-holds/{slotReservationId}/cancel` — cancel HOLD по `bookingId`.

## 5.2 Зоны

- `POST /zones/{zoneId}/holds` — HOLD зоны (`bookingId`, `slotReservationId`).
- `POST /zone-holds/{zoneReservationId}/confirm` — confirm HOLD зоны.
- `POST /zone-holds/{zoneReservationId}/cancel` — cancel зоны.
- `POST /zone-reservations/{zoneReservationId}/extend` — продление зоны (`bookingId`, `extendMinutes`).

---

## 6. Ключевая бизнес-логика по операциям

## 6.1 HOLD слота (`SlotService.holdSlot`)

Алгоритм:

1. Заблокировать строку `game_slot` (`findByIdForUpdate`).
2. Проверить существование слота.
3. Проверить `slot.status == OPEN`.
4. Посчитать активные резервации (`countActive`) по правилу:
    - `CONFIRMED` + неистёкшие `HOLD`.
5. Если `active >= capacityCompanies` → `SLOT_FULL`.
6. Иначе создать `slot_reservation` со статусом `HOLD` и `expiresAt = now + holdMinutes`.

Сильная сторона: валидация capacity делается в транзакции и с блокировкой слота.

## 6.2 CONFIRM HOLD слота (`SlotService.confirmHold`)

1. Найти резервацию по `(reservationId, bookingId)`.
2. Проверить, что текущий статус = `HOLD`.
3. Проверить, что `expiresAt` существует и не истёк.
4. Перевести в `CONFIRMED`, `expiresAt = null`.

Если любой шаг не пройден — бизнес-ошибка (`SLOT_HOLD_NOT_FOUND`, `INVALID_STATUS`, `HOLD_EXPIRED`).

## 6.3 CANCEL HOLD слота (`SlotService.cancelHold`)

1. Найти по `(reservationId, bookingId)`.
2. Если уже `CANCELLED` — no-op (идемпотентно).
3. Иначе проставить `CANCELLED`.

## 6.4 HOLD зоны (`ZoneService.holdZone`)

Алгоритм более сложный:

1. Заблокировать `zone` (`findByIdForUpdate`), проверить существование.
2. Заблокировать `slot_reservation` по `slotReservationId`.
3. Проверить, что `slotReservation.bookingId == bookingId`.
4. Проверить, что статус `slotReservation` = `HOLD`/`CONFIRMED`, а `HOLD` не истёк (`expiresAt > now`).
5. Проверить `zone.isActive == true`.
6. По `slotReservation.gameSlotId` заблокировать `game_slot`, проверить существование.
7. Проверить, что `game_slot.end_time` ещё в будущем (нельзя HOLD зоны для завершённого слота).
8. Рассчитать интервал зоны:
    - `start = gameSlot.startTime`;
    - `end = start + defaultDurationMinutesForZones`.
9. Посчитать пересечения (`countOverlaps`) по зоне на интервале `[start, end)` среди:
    - `ACTIVE`;
    - неистёкших `HOLD`.
10. Если `overlaps >= zone.capacityCompanies` → `ZONE_BUSY`.
11. Создать `zone_reservation` со статусом `HOLD`, `expiresAt = now + holdMinutes`, и ссылкой `slotReservationId`.

Важно: зона в текущей модели удерживается не произвольным интервалом из API, а автоматически от времени слота + фиксированная длительность из конфигурации.

## 6.5 CONFIRM HOLD зоны (`ZoneService.confirmZoneHold`)

1. Заблокировать `zone_reservation`.
2. Проверить совпадение `bookingId`.
3. Проверить статус `HOLD`.
4. Проверить, что HOLD не истёк.
5. Заблокировать `zone`, проверить существование.
6. Повторно проверить конфликты `countOverlaps(...)`, исключив текущую запись `excludeId = self`.
7. Если ок — `status = ACTIVE`, `expiresAt = null`.

Повторная проверка в confirm защищает от ситуации, когда между HOLD и CONFIRM появились новые конфликты.

## 6.6 CANCEL зоны (`ZoneService.cancelZoneHold`)

1. Заблокировать `zone_reservation`.
2. Проверить `bookingId`.
3. Если уже `CANCELLED` — идемпотентный return.
4. Иначе `status = CANCELLED`, `expiresAt = null`.

Метод работает не только для HOLD, но и для иных статусов (кроме уже `CANCELLED`) — т.е. позволяет отменить и ACTIVE резервацию.

## 6.7 Продление зоны (`ZoneService.extend`)

1. Заблокировать базовую резервацию.
2. Проверить `bookingId`.
3. Разрешить продление только из `ACTIVE`.
4. Рассчитать `targetEnd = base.endTime + extendMinutes` (в текущем API только этот режим).
5. Проверить, что `targetEnd > base.endTime`.
6. Заблокировать `zone`.
7. Проверить конфликты на интервале `[base.endTime, targetEnd)`.
8. Если нет конфликта — создать новую `ZoneReservation`:
    - `start_time = base.endTime`;
    - `end_time = targetEnd`;
    - `status = ACTIVE`;
    - `parent_reservation_id = base.id`;
    - `slot_reservation_id` наследуется.

Продление реализовано как append-chain, а не update существующей записи — это хорошо для аудита истории.

---

## 7. Конкурентность, атомарность и консистентность

Сервис в критичных местах использует:

- `@Transactional` на use-case методах;
- `PESSIMISTIC_WRITE` на читаемых строках перед проверками.

Что это даёт:

- защита от oversell по capacity слотов при одновременных HOLD;
- сериализация конфликтных операций по одной зоне/слоту;
- уменьшение race-condition между проверкой и вставкой.

Оставшийся риск: в `confirmHold` слота нет повторной проверки capacity (логически обычно и не нужно, т.к. HOLD уже занял емкость), но это означает, что корректность опирается на корректность phase HOLD.

---

## 8. Валидации запросов

Реализовано:

- `@NotNull bookingId` для hold slot / confirm / cancel;
- `@NotNull bookingId` и `@NotNull slotReservationId` для hold zone;
- централизованный ответ на validation ошибки (`VALIDATION_ERROR`).

На DTO-уровне добавлена валидация:

- в `ExtendZoneRequest` теперь есть `@NotNull` для `bookingId` и `extendMinutes`;
- `extendMinutes` помечен `@Positive` (дополнительно к бизнес-проверке в сервисе).

---

## 9. Ошибки и error contract

Единый формат:

- `code`;
- `message`;
- `details` (map).

Типы:

- `NotFoundException` -> HTTP 404;
- остальные `BusinessException` -> HTTP 400;
- неожиданные ошибки -> HTTP 500 c `errorId`.

Примеры бизнес-кодов:

- `SLOT_NOT_FOUND`, `SLOT_NOT_OPEN`, `SLOT_FULL`, `HOLD_EXPIRED`;
- `ZONE_NOT_FOUND`, `ZONE_INACTIVE`, `ZONE_BUSY`;
- `BOOKING_MISMATCH`, `INVALID_STATUS`, `INVALID_TIME`, `INVALID_REQUEST`.

---

## 10. Конфигурация, влияющая на бизнес-поведение

Через `pb.scheduling`:

- `hold-cleanup-interval-ms` — период запуска cleanup job для просроченных HOLD;
- `hold-minutes` — TTL HOLD (слоты и зоны);
- `default-duration-minutes_for_zones` — стандартная длительность зоны от старта слота.

В коде свойства читаются как `holdMinutes()` и `defaultDurationMinutesForZones()`, т.е. конфиг напрямую влияет на:

- окно, в которое HOLD можно подтвердить;
- длину резервирования зоны при операции HOLD.

---

## 11. Интеграционный сценарий с booking-service (как это работает сейчас)

Де-факто контракт в текущем коде:

1. booking-service создаёт HOLD слота (`bookingId`).
2. Получает `slotReservationId`.
3. При необходимости создаёт HOLD зоны, передавая `bookingId + slotReservationId`.
4. На подтверждении брони вызывает confirm для слота/зоны.
5. На отмене брони вызывает cancel для слота/зоны.

Ключевая связка: `bookingId` + `slotReservationId`.

---

## 12. Сравнение с вашим исходным ТЗ: что совпало, а что эволюционировало

### Совпадает

- модель статусов для slot/zone reservations;
- capacity по слоту через active reservations;
- интервалный конфликт по зоне через `startA < endB && endA > startB`;
- транзакционность и блокировки на критичных операциях;
- продление зоны отдельной записью с `parent_reservation_id`.

### Изменилось/уточнилось

- HOLD зоны привязан к `slotReservationId` и времени игрового слота, а не принимает `start/end` напрямую;
- endpoint’ы админки и расширенный листинг (`/arenas`, `/zones`, фильтры, PATCH и т.д.) пока не реализованы;
- в extend реализован режим `extendMinutes`; режима `newEndTime` пока нет;
- `FINISHED` есть в enum, но не используется в сервисе;
- миграции Flyway заявлены, но в модуле в данный момент не присутствуют.

---

## 13. Нюансы, о которых важно знать перед дальнейшим развитием

1. В `holdZone` теперь есть проверка, что `slotReservation.bookingId == bookingId` запроса (защита от чужих bookingId).
2. В `holdZone` добавлена проверка статуса `slotReservation` (`HOLD`/`CONFIRMED`) и отдельная проверка, что `HOLD` не истёк (`expiresAt > now`).
3. Для зон используется фиксированная длительность из конфига, а не гибкий интервал с клиента.
4. Добавлен job, который переводит просроченные HOLD в `CANCELLED` (периодический cleanup через `@Scheduled`).
5. В `ExtendZoneRequest` добавлены bean-validation аннотации (`@NotNull`, `@Positive`).

---

## 14. Итог

Текущий `scheduling-app` уже реализует ядро MVP-бизнес-логики для бронирования ресурсов:

- атомарное удержание слотов по capacity;
- подтверждение/отмена HOLD;
- конфликт-менеджмент по зонам с учётом пересечений и лимита компаний;
- продления зоны как цепочку самостоятельных интервалов.

Главная эволюция относительно исходного ТЗ — связка зон с уже созданной `slot_reservation` и автоматическое вычисление интервала зоны от слота + конфигурации.

Это делает текущую версию удобной для сценария: «бронь игры -> затем опциональная зона на стандартный интервал».
