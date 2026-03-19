# ТЗ — Scheduling Service

## 1. Назначение
Управление расписанием и ресурсами: арены, слоты и зоны, удержания, конфликты, продления.
Сервис гарантирует, что ресурс не будет занят дважды, и ведёт историю действий по удержаниям.

## 1.1 Бизнес-процессы (словами)
- Администратор заводит арены и расписание доступных слотов/зон.
- При создании бронирования сервис удерживает слот/зону на ограниченное время.
- При подтверждении брони удержание становится активным, ресурс считается занятым.
- При отмене или истечении удержания ресурс освобождается для других клиентов.
- Если визит продлевается, сервис проверяет конфликт и увеличивает время брони.

## 2. `resource_lock_log/audit` — зачем
Нужен для расследования спорных ситуаций: кто и когда занял/освободил ресурс.

Хранить:
- `resource_type` (SLOT/ZONE)
- `resource_id`
- `action` (HOLD/CONFIRM/CANCEL/EXPIRE/EXTEND)
- `booking_id`
- `actor_id` (кто инициировал)
- `created_at`
- `trace_id`

## 3. Статусы
- `game_slot.status`: `OPEN`, `CLOSED`, `CANCELLED`
- `slot_reservation.status`: `HOLD`, `CONFIRMED`, `CANCELLED`, `EXPIRED`
- `zone_reservation.status`: `HOLD`, `ACTIVE`, `CANCELLED`, `FINISHED`, `EXPIRED`

## 4. API

### Zones CRUD
| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/zones` | Создание зоны |
| `GET` | `/zones` | Список зон (`?active=true/false`) |
| `GET` | `/zones/{zoneId}` | Получение зоны по ID |
| `PATCH` | `/zones/{zoneId}` | Обновление зоны |
| `DELETE` | `/zones/{zoneId}` | Деактивация зоны (soft delete) |

**Поля зоны:**

| Поле | Тип | Обязательность | Описание |
|------|-----|----------------|----------|
| `code` | VARCHAR | обязательно (уникальный) | Короткий идентификатор зоны |
| `name` | VARCHAR | обязательно | Отображаемое имя |
| `type` | ENUM | обязательно | `REST` или `DRESSING` |
| `capacityCompanies` | INT | обязательно, > 0 | Сколько компаний может занимать зону одновременно |
| `paid` | BOOLEAN | опционально | Является ли зона платной (default: `true`) |
| `active` | BOOLEAN | auto | Активна ли зона (default: `true`) |

> `DELETE` не удаляет запись физически — выставляет `active = false`. Существующие брони не затрагиваются.

### Slots & Holds (существующее)
- Arenas: `GET/POST/PATCH /arenas`
- Slots: `GET/POST/PATCH /slots`
- Holds: `POST /slots/{slotId}/holds`
- Confirm: `POST /slot-holds/{id}/confirm`
- Cancel: `POST /slot-holds/{id}/cancel`
- Zone holds: `POST /zones/{zoneId}/holds`
- Zone confirm/cancel: `POST /zone-holds/{id}/confirm|cancel`
- Extend: `POST /zone-reservations/{id}/extend`

## 5. Полная валидация
- `bookingId` обязателен
- `holdMinutes` в диапазоне (например 1..60)
- `endTime > startTime`
- `extend` не уменьшает время
- запрет confirm для просроченного hold
- slot/zone должны существовать и быть активными

## 6. Политика ошибок
- `SLOT_NOT_FOUND` (404)
- `SLOT_CLOSED` (409)
- `SLOT_FULL` (422)
- `SLOT_HOLD_EXPIRED` (409)
- `ZONE_NOT_FOUND` (404)
- `ZONE_BUSY` (422)
- `ZONE_EXTEND_CONFLICT` (422)
- `RESERVATION_NOT_FOUND` (404)
- `RESERVATION_INVALID_STATE` (409)
- `VALIDATION_ERROR` (400)

## 7. Бизнес-правила
- Транзакционность на hold/confirm/cancel.
- `SELECT ... FOR UPDATE` для capacity check.
- Повторные confirm/cancel idempotent.
