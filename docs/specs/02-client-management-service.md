# ТЗ — Client Management Service

## 1. Назначение
CRM-ядро клиентов клуба: контакты, согласия, теги, связь с будущим личным кабинетом.
Сервис хранит «карточку клиента» и является источником истины по персональным данным и сегментации.

## 1.1 Бизнес-процессы (словами)
- При первом обращении клиента администратор создаёт карточку и фиксирует контакты.
- Клиент даёт согласия (ПДн/маркетинг/SMS), сервис сохраняет факт, дату и источник.
- По мере взаимодействия клиенту назначают теги для сегментации (VIP, день рождения и т.п.).
- Контакты обновляются, чтобы в бронировании/оплате всегда были актуальные данные.
- При необходимости карточка архивируется, но история согласий остаётся неизменной.

## 2. Зачем нужны сущности
- `client_consent`: хранит юридически значимые согласия (ПДн/маркетинг/SMS), когда и откуда получено.
- `client_tag`: словарь меток для сегментации (VIP, конфликтный, день рождения и т.п.).
- `client_tag_link`: связь many-to-many между клиентом и тегами.
- `client_identity_link` (optional): связывает CRM-клиента с auth-аккаунтом (для будущей регистрации/логина клиента).

## 3. Сущности
- `client`
- `client_contact`
- `client_consent`
- `client_tag`
- `client_tag_link`
- `client_identity_link` (optional)

## 4. API
- `POST /clients`
- `POST /clients/upsert`
- `GET /clients/{id}`
- `GET /clients`
- `PATCH /clients/{id}`
- `POST /clients/{id}/archive`
- `POST /clients/{id}/contacts`
- `PATCH /clients/{id}/contacts/{contactId}`
- `POST /clients/{id}/consents`
- `GET /clients/{id}/consents`
- `GET /tags`
- `POST /tags`
- `POST /clients/{id}/tags/{tagId}`

## 5. Полная валидация
- хотя бы один контакт (phone/email)
- primary phone/email уникален (в нормализованном виде)
- consent.type из разрешённого списка
- archive запрещён, если есть активное бронирование (или предупреждение по политике)
- tag name уникален в рамках системы

## 6. Политика ошибок
- `CLIENT_NOT_FOUND` (404)
- `CLIENT_DUPLICATE_CONTACT` (409)
- `CLIENT_ARCHIVED` (409)
- `CLIENT_CONTACT_INVALID` (400)
- `CLIENT_CONSENT_INVALID` (400)
- `CLIENT_TAG_NOT_FOUND` (404)
- `CLIENT_TAG_ALREADY_ASSIGNED` (409)
- `VALIDATION_ERROR` (400)

## 7. Бизнес-правила
- История consent должна быть неизменяемой (append-only).
- Upsert всегда возвращает финальный `clientId`.
- В логах контакты маскируются.
