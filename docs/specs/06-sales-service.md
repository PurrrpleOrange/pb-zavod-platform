# ТЗ — Sales Service

## 1. Назначение
Управление счётом: позиции, оплата, закрытие. Сервис фиксирует состав продажи,
снимает оплату, хранит историю платежей и при необходимости интегрируется с фискализацией.

## 1.1 Бизнес-процессы (словами)
- При подтверждённой брони открывается счёт на оплату услуг/товаров.
- В счёт добавляются позиции (услуги, продукты), фиксируются цены snapshot-ом.
- Клиент оплачивает, сервис проверяет сумму, фиксирует платёж и закрывает счёт.
- При подключении кассы формируется фискальный чек и хранится его статус.

## 2. Сущности
- `sale`
- `sale_item`
- `sale_payment`
- `fiscal_receipt` (future-ready)

## 3. Что такое `fiscal_receipt`
Запись о фискальном чеке для ККТ/ОФД:
- номер чека
- дата/время
- сумма
- статус фискализации
- внешний идентификатор кассы/ОФД

Если фискализация пока не подключена — таблица может быть optional и пустой.

## 4. Статусы sale
- `OPEN`
- `CLOSED`
- `CANCELLED`
- `REFUNDED` (future)

`REFUNDED` — счёт, по которому выполнен полный возврат средств.

## 5. API
- `POST /sales/open`
- `GET /sales?bookingId=`
- `GET /sales/{id}`
- `POST /sales/{id}/items`
- `DELETE /sales/{id}/items/{itemId}` (optional)
- `POST /sales/{id}/pay`

## 6. Полная валидация
- один `OPEN` sale на booking
- quantity > 0
- unit_price_snapshot > 0
- pay.amount > 0
- paid >= amount
- запрет item mutations после `CLOSED`

## 7. Политика ошибок
- `SALE_NOT_FOUND` (404)
- `SALE_ALREADY_OPEN` (409)
- `SALE_INVALID_STATE` (409)
- `SALE_ITEM_NOT_FOUND` (404)
- `SALE_PAYMENT_ALREADY_EXISTS` (409)
- `SALE_PAYMENT_AMOUNT_INVALID` (400)
- `SALE_PAYMENT_INSUFFICIENT` (422)
- `VALIDATION_ERROR` (400)

## 8. Бизнес-правила
- Сумма и цены фиксируются snapshot-ом.
- При оплате фиксируется change = paid - amount.
- Повторная оплата одного счёта запрещена.
