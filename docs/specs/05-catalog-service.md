# ТЗ — Catalog Service

## 1. Назначение
Справочник игр, тарифов, продуктов/услуг. Сервис хранит правила тарифа и состав услуг,
которые используются в бронированиях и продажах, и задаёт основу для расчёта цены.

## 1.1 Бизнес-процессы (словами)
- Администратор создаёт и поддерживает каталог игр, тарифов и продуктов.
- При настройке тарифа задаются включённые позиции и правила расчёта.
- Бронирование и продажи читают данные каталога для расчёта итоговой цены.
- При изменении прайса новые брони используют новые тарифы, старые сохраняют snapshot.

## 2. Сущности MVP
- `game_type`
- `tariff`
- `tariff_included_item`
- `tariff_addon`
- `product`

## 3. API
- `GET /game-types`
- `GET /tariffs`
- `GET /tariffs/{id}`
- `GET /products`
- Admin CRUD для тарифов и продуктов

## 4. Полная валидация
- тариф привязан к существующему game_type
- included item: quantity_per_player
- product price > 0
- деактивация запрещена для сущностей в активных ссылках (или soft-policy)

## 5. Политика ошибок
- `CATALOG_GAME_TYPE_NOT_FOUND` (404)
- `CATALOG_TARIFF_NOT_FOUND` (404)
- `CATALOG_TARIFF_INACTIVE` (409)
- `CATALOG_PRODUCT_NOT_FOUND` (404)
- `CATALOG_PRODUCT_INACTIVE` (409)
- `VALIDATION_ERROR` (400)

## 6. Roadmap (будущее: учёт инвентаря)
Пока не делать в MVP, но запланировать:
1. `inventory_item` (номенклатура)
2. `inventory_balance` (остатки)
3. `inventory_movement` (приход/расход/списание)
4. `inventory_supplier`
5. `inventory_writeoff_reason`
6. Автосписание расходников из продаж/операций

При росте сложности вынести в отдельный `inventory-service`.
