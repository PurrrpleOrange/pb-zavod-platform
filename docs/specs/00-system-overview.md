# ТЗ v2 — Общее (Platform Overview)

## 1. Формат ТЗ
Этот документ — **общее ТЗ** (архитектура, сквозные требования, интеграция).
Для реальной разработки используются **отдельные ТЗ по каждому сервису**:
- `01-auth-user-service.md`
- `02-client-management-service.md`
- `03-scheduling-service.md`
- `04-booking-service.md`
- `05-catalog-service.md`
- `06-sales-service.md`

> Решение: идти по варианту "общее + отдельные ТЗ под каждый сервис".

## 2. Сервисы в MVP+
1. Auth/User Service
2. Client Management Service
3. Scheduling Service
4. Booking Service
5. Catalog Service
6. Sales Service

## 3. Роли (упрощённо под небольшой клуб)
Минимальный набор ролей:
- `ADMIN` — администратор клуба (включая владельца клуба)
- `INSTRUCTOR` — инструктор клуба
- `TRAINEE` *(опционально)* — стажёр с урезанными правами

RBAC хранится в Auth/User Service.

## 4. Что такое multi-tenant (простое объяснение)
- **Single-tenant**: система обслуживает 1 клуб.
- **Multi-tenant**: одна система обслуживает несколько клубов (арендаторов/тенантов), данные логически разделены.

Для текущего этапа: **single-tenant**, без усложнений.

## 5. События (обязательный минимум)
Обязательные доменные события на старте:
- `booking.created`
- `booking.confirmed`
- `booking.cancelled`
- `scheduling.slot.held`
- `scheduling.zone.held`
- `client.registered`
- `client.authenticated`
- `staff.authenticated`

## 6. Snapshot vs ретропересчёт (простое объяснение)
- **Snapshot**: цена фиксируется в момент операции и не меняется задним числом.
- **Ретропересчёт**: старые операции могут "пересчитаться" после изменения тарифа/цены.

Для MVP: использовать **snapshot** в booking и sales.

## 7. Онлайн-касса и `fiscal_receipt`
`fiscal_receipt` — сущность фискального чека (номер, время, сумма, фискальный признак, статус отправки в ОФД).
Нужна, если подключается легальная фискализация/ККТ.

В MVP можно хранить "техническую" оплату без фискализации, а `fiscal_receipt` держать как future-ready модель.

## 8. Нефункциональные требования (подробно)
### 8.1 Производительность
- Цель для API:
    - p95 read < 300 ms
    - p95 write < 500 ms
- Отдельный KPI для тяжёлых отчётов: до 2–3 сек в админке.

### 8.2 Доступность
- MVP: 99.5% uptime/месяц.
- После стабилизации: 99.9%.
- Health checks (`/actuator/health`) обязательны.

### 8.3 Наблюдаемость (Observability)
- Структурированные логи JSON.
- В каждом запросе: `traceId`, `requestId`, `actorId`.
- Метрики: latency, error rate, saturation, business counters (holds/confirm/cancel/pay).

### 8.4 Безопасность
- TLS между внешними клиентами и gateway.
- JWT для staff/client с коротким TTL.
- Refresh token rotation.
- Пароли: Argon2/Bcrypt.
- Rate-limit на auth endpoints.

### 8.5 Данные и резервное копирование
- Отдельная PostgreSQL БД на сервис.
- Бэкапы: ежедневный full + WAL/incremental.
- Тест восстановления минимум 1 раз в месяц.

### 8.6 Миграции и совместимость
- Все изменения схемы через Flyway/Liquibase.
- Backward-compatible подход для API и БД (без ломающих изменений "внезапно").

### 8.7 Надёжность интеграций
- Idempotency-Key для create/confirm/cancel/pay.
- Ретраи с exponential backoff на межсервисных вызовах.
- Таймауты и circuit breaker.
- Для событий: outbox pattern.

### 8.8 Защита ПДн
- Маскирование телефонов/email в логах.
- Ограничение доступа по ролям.
- Аудит изменений критичных данных.

## 9. Политика ошибок (единая и полная)
Формат ответа ошибки:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "field": "phone",
    "reason": "INVALID_FORMAT"
  }
}
```

### 9.1 Категории кодов
- `VALIDATION_*` — ошибки валидации
- `AUTH_*` — аутентификация/авторизация
- `BOOKING_*` — ошибки бронирований
- `SLOT_*`, `ZONE_*` — scheduling ошибки
- `SALE_*`, `PAYMENT_*` — касса/оплата
- `CLIENT_*` — клиентские данные
- `CATALOG_*` — справочники/цены
- `IDEMPOTENCY_*` — повторные/конфликтные запросы
- `INTEGRATION_*` — ошибки внешних сервисов
- `INTERNAL_ERROR` — непредвиденная ошибка

### 9.2 HTTP mapping
- 400 — validation
- 401 — unauthenticated
- 403 — forbidden
- 404 — not found
- 409 — conflict/state conflict
- 422 — business rule violation
- 429 — rate limit
- 500 — internal
- 503 — dependency unavailable

## 10. Идеи на будущее (инвентарь/учёт имущества)
Пока **не в MVP**, но roadmap:
1. Инвентарь расходников (напитки, снеки, швабры, тряпки и пр.)
2. Складские движения: приход/списание/перемещение
3. Нормы расхода по услугам и сменам
4. Инвентаризация и акты расхождений
5. Интеграция с продажами (автосписание)

Когда объём вырастет — вынести в отдельный `inventory-service`.
