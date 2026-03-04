# ТЗ — Booking Service

## 1. Назначение
Оркестрация процесса бронирования клиента. Сервис связывает клиента, слот/зону и тариф,
создаёт бронь, инициирует удержание/подтверждение в scheduling и фиксирует цену на момент создания.

## 1.1 Бизнес-процессы (словами)
- Администратор выбирает клиента, слот/зону и тариф и создаёт бронь.
- Сервис запрашивает удержание ресурса в scheduling и фиксирует цену snapshot-ом.
- После подтверждения брони статус меняется на `CONFIRMED`, ресурс закрепляется.
- При отмене брони сервис освобождает ресурс и фиксирует причину отмены.
- После фактического визита бронь закрывается как `COMPLETED` (или `NO_SHOW` при неявке).

## 2. Статусы и зачем они нужны
- `DRAFT` (optional): черновик до удержания слота.
- `HOLD`: слот/зона удержаны, но ещё не финально подтверждены.
- `CONFIRMED`: бронь подтверждена, ресурсы закреплены.
- `CANCELLED`: бронь отменена, ресурсы освобождены.
- `COMPLETED`: визит завершён, услуга оказана.
- `NO_SHOW` (optional): клиент не пришёл, нужно для аналитики и штрафов.
- `REFUNDED` (future): деньги по брони возвращены.

## 3. API
- `POST /bookings`
- `POST /bookings/{id}/confirm`
- `POST /bookings/{id}/cancel`
- `POST /bookings/{id}/complete`
- `POST /bookings/{id}/no-show` (optional)
- `GET /bookings/{id}`
- `GET /bookings`

## 4. Полная валидация
- clientId обязателен
- gameSlotId обязателен
- tariffId обязателен
- playersCount > 0
- confirm только из `HOLD`
- cancel только из `HOLD/CONFIRMED`
- complete только из `CONFIRMED`

## 5. Политика ошибок
- `BOOKING_NOT_FOUND` (404)
- `BOOKING_INVALID_STATE` (409)
- `BOOKING_SLOT_HOLD_FAILED` (422)
- `BOOKING_ZONE_HOLD_FAILED` (422)
- `BOOKING_CLIENT_INVALID` (400/404)
- `BOOKING_TARIFF_INVALID` (400/404)
- `BOOKING_PREPAYMENT_REQUIRED` (422)
- `VALIDATION_ERROR` (400)

## 6. Бизнес-правила
- `total_price_snapshot` фиксируется при создании.
- Отмена обязана инициировать освобождение ресурса в scheduling.
- Повторный `confirm` и `cancel` — idempotent.
