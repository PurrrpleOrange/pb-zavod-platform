# scheduling-service

Сервис планирования ресурсов клуба: игровые слоты (площадки) и зоны (раздевалка/беседки и т.п.).
Отвечает за **временные интервалы**, **вместимость (по компаниям)** и **конфликты**.

## Responsibilities (MVP)
- Управление расписанием `game_slot` по аренам
- Резервирование слотов компаниями (`slot_reservation`) с **HOLD/CONFIRM/CANCEL**
- Резервирование зон (`zone_reservation`) с **HOLD/CONFIRM/CANCEL/FINISH**
- Проверка конфликтов (capacity и пересечения интервалов)
- Продление зоны (цепочка продлений через `parent_reservation_id`)

> В MVP **объединение площадок не поддерживается**.

---

## Domain model

### Arena
Справочник физических площадок.

- `id: UUID`
- `name: String`
- `active: boolean` — глобальная доступность площадки (ремонт/закрытие)

### GameSlot
Временной интервал на конкретной арене с лимитом компаний.

- `id: UUID`
- `arenaId: UUID`
- `startTime: timestamptz`
- `endTime: timestamptz`
- `capacityCompanies: int` (обычно 2)
- `status: OPEN | CLOSED | CANCELLED`

**Semantics**
- `OPEN` — слот доступен для HOLD
- `CLOSED` — слот заблокирован (нельзя бронировать)
- `CANCELLED` — слот отменён как сущность расписания (история/аудит)

### SlotReservation
Занятость одного места в слоте одной компанией (единица capacity).

- `id: UUID`
- `gameSlotId: UUID`
- `bookingId: UUID` (внешний идентификатор booking-service)
- `status: HOLD | CONFIRMED | CANCELLED`
- `createdAt: timestamptz`
- `expiresAt: timestamptz` (обязателен для HOLD)

**Active capacity**
- `CONFIRMED`
- `HOLD` где `expiresAt > now()`

### Zone
Справочник зон.

- `id: UUID`
- `code: String` (unique)
- `name: String`
- `type: REST | DRESSING`
- `capacityCompanies: int`
- `paid: boolean`
- `active: boolean`

### ZoneReservation
Бронирование зоны интервалом времени + поддержка продлений.

- `id: UUID`
- `zoneId: UUID`
- `bookingId: UUID`
- `startTime: timestamptz`
- `endTime: timestamptz`
- `status: HOLD | ACTIVE | CANCELLED | FINISHED`
- `createdAt: timestamptz`
- `expiresAt: timestamptz` (обязателен для HOLD)
- `parentReservationId: UUID?` (если это продление)

**Conflict rule**
Считаем пересечения по:
- `ACTIVE`
- `HOLD` где `expiresAt > now()`

Конфликт, если количество пересечений `>= zone.capacityCompanies`.

**Extend**
Продление = новая запись `ZoneReservation`:
- `startTime = base.endTime`
- `endTime = newEndTime`
- `parentReservationId = base.id`
- `status = ACTIVE`

---

## Business rules

### Slot hold
- HOLD возможен только если `game_slot.status = OPEN`
- При HOLD делаем атомарную проверку capacity:
  - активные = `CONFIRMED` + `HOLD (expiresAt > now)`
  - если `active >= capacityCompanies` → `SLOT_FULL`
- HOLD создаётся с `expiresAt = now + holdMinutes`

### Slot confirm/cancel
- CONFIRM: `HOLD -> CONFIRMED` (если не истёк)
- CANCEL: перевод в `CANCELLED` (идемпотентно)

### Zone hold / confirm / cancel / finish
- HOLD: проверка пересечений и capacity по зоне
- CONFIRM: `HOLD -> ACTIVE`
- CANCEL: `-> CANCELLED` (идемпотентно)
- FINISH: опционально (по админке или job), либо “фактическое завершение”

---

## API (to be filled)
> Ниже — место под спецификацию эндпоинтов. Заполняем после реализации контроллеров.

### Slots
- `POST /slots/{slotId}/holds`
- `POST /slot-holds/{reservationId}/confirm`
- `POST /slot-holds/{reservationId}/cancel`

### Zones
- `POST /zones/{zoneId}/holds`
- `POST /zone-holds/{reservationId}/confirm`
- `POST /zone-holds/{reservationId}/cancel`
- `POST /zone-reservations/{reservationId}/extend`

### Admin (planned)
- CRUD: arenas, zones
- Management: open/close/cancel game slots
- Generate schedule: create slots for period (week/month)

---

## Integration with booking-service (planned contract)
Scheduling-service является источником истины по занятости ресурсов.

Booking-service:
- при создании брони:
  - HOLD slot
  - HOLD zone (если выбрана)
- при подтверждении/отмене:
  - confirm/cancel соответствующие резервации
    Связь выполняется по `bookingId` (без FK).

---

## Database & migrations
- PostgreSQL
- Flyway migrations in: `src/main/resources/db/migration`
сами зайдите в папку да посмотрите миграции (в будущем напишу)

---

## Local development

### Run PostgreSQL
```bash
docker compose up -d
```

### Run service

```bash
mvn -pl scheduling-app spring-boot:run
```

### Roadmap (planned)

- Seed data: arenas, zones, schedule template
- Admin endpoints for schedule generation + slot status changes
- Expiration jobs:
  - cancel expired HOLDs
  - finish outdated zone reservations
- Observability:
  - structured logs
  - metrics
- API docs via OpenAPI/Swagger