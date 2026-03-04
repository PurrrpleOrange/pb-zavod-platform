# ТЗ — Auth/User Service

## 1. Назначение
Аутентификация и авторизация сотрудников клуба. Сервис управляет учётными записями,
выдаёт и обновляет токены, проверяет роли и права доступа, хранит аудит входов и изменений ролей.

## 1.1 Бизнес-процессы (словами)
- Администратор создаёт учётную запись сотрудника и назначает роли доступа.
- Сотрудник проходит логин: сервис проверяет учётные данные и выдаёт access/refresh токены.
- В процессе работы сервис проверяет токены и права на доступ к защищённым операциям.
- При увольнении/блокировке администратор отключает аккаунт, вход становится невозможен.
- Все критичные действия (входы, смена ролей) пишутся в аудит.

## 2. Роли
- `ADMIN`
- `INSTRUCTOR`
- `TRAINEE` (optional)

## 3. Статусы пользователя
- `ACTIVE` — пользователь активен, вход разрешён.
- `DISABLED` — учётная запись отключена админом, вход запрещён.
- `LOCKED` (optional) — временная автоматическая блокировка после N неуспешных логинов.

> Если не нужна автоблокировка, `LOCKED` можно не вводить в MVP.

## 4. Сущности
- `auth_user`
- `auth_role`
- `auth_permission`
- `auth_user_role`
- `auth_refresh_token`
- `auth_audit_log`

## 5. API
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /auth/password/change`
- `POST /users`
- `GET /users`
- `PATCH /users/{id}`
- `POST /users/{id}/roles`
- `DELETE /users/{id}/roles/{role}`

## 6. Полная валидация
- login обязателен
- password обязателен
- длина password >= 8
- email валидного формата
- phone в нормализованном формате
- роль должна существовать
- нельзя удалить последнего `ADMIN`

## 7. Политика ошибок
- `AUTH_INVALID_CREDENTIALS` (401)
- `AUTH_ACCOUNT_DISABLED` (403)
- `AUTH_ACCOUNT_LOCKED` (423/403)
- `AUTH_TOKEN_EXPIRED` (401)
- `AUTH_TOKEN_INVALID` (401)
- `AUTH_REFRESH_REUSED` (401)
- `AUTH_ROLE_NOT_FOUND` (404)
- `AUTH_PERMISSION_DENIED` (403)
- `VALIDATION_ERROR` (400)
- `IDEMPOTENCY_CONFLICT` (409)

## 8. Бизнес-правила
- Login и refresh idempotent по смыслу с контролем сессий.
- Смена ролей логируется в `auth_audit_log`.
- Пароли не возвращаются ни в каком API.
