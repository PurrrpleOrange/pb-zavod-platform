# Auth User Service

IAM (Identity and Access Management) сервис для сотрудников клуба.

Сервис отвечает за аутентификацию, авторизацию и управление учетными записями сотрудников внутренней системы.

---

# Table of Contents

* [1. Назначение сервиса](#1-назначение-сервиса)
* [1.1 Бизнес-процессы](#11-бизнес-процессы)
* [2. Роли и статусы](#2-роли-и-статусы)
* [3. Границы ответственности](#3-границы-ответственности)
* [4. Доменная модель и база данных](#4-доменная-модель-и-база-данных)
* [5. API контракт](#5-api-контракт)
* [6. Валидация](#6-валидация)
* [7. Политика ошибок](#7-политика-ошибок)
* [8. Security blueprint](#8-security-blueprint)
* [9. Аудит и наблюдаемость](#9-аудит-и-наблюдаемость)
* [10. Идемпотентность и транзакционность](#10-идемпотентность-и-транзакционность)
* [11. Структура проекта](#11-структура-проекта)
* [12. Нефункциональные требования](#12-нефункциональные-требования)
* [13. План реализации](#13-план-реализации)

---

# 1. Назначение сервиса

`auth-user-service` отвечает за IAM-контур сотрудников клуба:

* аутентификация (login / refresh / logout)
* авторизация (роли и права)
* управление учетными записями сотрудников
* аудит security-критичных действий

---

# 1.1 Бизнес-процессы

Основные сценарии использования системы:

1. Администратор создаёт аккаунт сотрудника и назначает роли.
2. Сотрудник выполняет вход в систему.
3. Сервис проверяет credentials и выдаёт access/refresh токены.
4. Во время работы происходит проверка токена и прав доступа.
5. При блокировке или увольнении сотрудника вход запрещается.
6. Критичные действия фиксируются в audit-логе.

---

# 2. Роли и статусы

## 2.1 Роли (MVP)

* **ADMIN**
* **INSTRUCTOR**
* **TRAINEE** (опционально)

---

## 2.2 Статусы пользователя

| Статус   | Описание                                       |
| -------- | ---------------------------------------------- |
| ACTIVE   | вход разрешён                                  |
| DISABLED | вход запрещён администратором                  |
| LOCKED   | временная блокировка после N неудачных логинов |

---

# 3. Границы ответственности

## Внутри сервиса

* учетные записи сотрудников
* токены и сессии
* RBAC
* audit trail

## Вне сервиса

Ответственность других сервисов:

* каталог услуг
* бронирования
* расписание
* клиентские профили

---

# 4. Доменная модель и база данных

## Таблица: auth_user

Хранит учетные записи сотрудников.

Поля:

* id (UUID, PK)
* login (UNIQUE, NOT NULL)
* email (UNIQUE)
* phone
* password_hash
* status (ACTIVE / DISABLED / LOCKED)
* failed_login_attempts
* last_login_at
* created_at
* updated_at
* created_by
* updated_by
* version (optimistic locking)

---

## Таблица: auth_role

Роли системы.

Поля:

* id
* code (ADMIN / INSTRUCTOR / TRAINEE)
* name
* description
* is_system
* audit поля

---

## Таблица: auth_permission

Разрешения системы.

Пример:

```
users.read
users.write
roles.manage
```

Поля:

* id
* code (UNIQUE)
* description

---

## Таблица: auth_user_role

Связь пользователь ↔ роль.

Поля:

* user_id → auth_user.id
* role_id → auth_role.id
* assigned_at
* assigned_by

Constraint:

```
UNIQUE(user_id, role_id)
```

---

## Таблица: auth_refresh_token

Хранит refresh токены и сессии пользователей.

Поля:

* id
* user_id
* token_hash
* jti
* issued_at
* expires_at
* revoked_at
* replaced_by_jti
* device_id
* ip
* user_agent

Индексы:

* user_id
* jti
* expires_at

---

## Таблица: auth_audit_log

Аудит действий системы.

Поля:

* id
* event_type
* actor_user_id
* target_user_id
* entity_type
* entity_id
* result
* error_code
* metadata (JSON / JSONB)
* created_at

Индексы:

* created_at
* actor_user_id
* target_user_id

---

## Дополнительно рекомендуется

Добавить таблицу:

```
auth_role_permission
```

Связь:

```
role → permission
```

---

# 5. API контракт

## Auth endpoints

### POST /auth/login

Аутентификация пользователя.

Проверяется:

* login
* password
* status
* блокировки

Ответ:

* access token
* refresh token
* пользовательский контекст

---

### POST /auth/refresh

Обновление access токена.

Поведение:

* refresh token rotation
* reuse detection

Ошибка при повторном использовании:

```
AUTH_REFRESH_REUSED
```

---

### POST /auth/logout

Инвалидирует refresh токен или сессию.

---

### POST /auth/password/change

Смена пароля.

Проверки:

* старый пароль
* политика нового пароля

После:

* новый пароль хешируется
* изменения сохраняются
* событие логируется

---

## User endpoints

### POST /users

Создание сотрудника.

Параметры:

* login
* email / phone
* пароль
* статус
* роли

---

### GET /users

Получение списка пользователей.

Поддерживает:

* пагинацию
* фильтрацию по status
* фильтрацию по role
* поиск

---

### PATCH /users/{id}

Частичное обновление:

* email
* phone
* status

---

### POST /users/{id}/roles

Назначение роли пользователю.

Проверка:

* роль должна существовать

---

### DELETE /users/{id}/roles/{role}

Удаление роли.

Правило:

Нельзя удалить **последнего активного ADMIN**.

---

# 6. Валидация

* login обязателен
* password обязателен
* длина password ≥ 8
* email должен быть валидным
* phone должен быть нормализован (E.164)
* роль должна существовать
* запрещено удаление последнего ADMIN

Дополнительно рекомендуется:

* trim и normalize login/email
* case-insensitive уникальность login/email
* запрет слабых паролей

---

# 7. Политика ошибок

Коды ошибок:

| Код                      | HTTP      |
| ------------------------ | --------- |
| AUTH_INVALID_CREDENTIALS | 401       |
| AUTH_ACCOUNT_DISABLED    | 403       |
| AUTH_ACCOUNT_LOCKED      | 423 / 403 |
| AUTH_TOKEN_EXPIRED       | 401       |
| AUTH_TOKEN_INVALID       | 401       |
| AUTH_REFRESH_REUSED      | 401       |
| AUTH_ROLE_NOT_FOUND      | 404       |
| AUTH_PERMISSION_DENIED   | 403       |
| VALIDATION_ERROR         | 400       |
| IDEMPOTENCY_CONFLICT     | 409       |

---

## Формат ошибки

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid login or password",
  "details": [],
  "traceId": "abc-123",
  "timestamp": "2026-03-04T12:00:00Z"
}
```

---

# 8. Security blueprint

## Пароли

* хранить только hash
* использовать bcrypt или argon2
* не логировать пароль или hash
* использовать безопасное сравнение

---

## Токены

Access token:

* TTL 10–20 минут

Refresh token:

* TTL 7–30 дней
* хранить только hash
* обязательный `jti`

Политики:

* refresh rotation
* reuse detection
* revoke цепочки токенов

---

## Блокировки

MVP:

```
DISABLED
```

Расширение:

```
LOCKED после N неудачных логинов
```

---

## Авторизация

Используется RBAC модель:

```
role → permission
```

Проверка прав выполняется:

* на уровне endpoint
* на уровне сервисного слоя

JWT содержит минимальные claims:

* sub
* roles
* jti
* exp

---

# 9. Аудит и наблюдаемость

## События аудита

* LOGIN_SUCCESS
* LOGIN_FAILED
* REFRESH_SUCCESS
* REFRESH_REUSED
* LOGOUT
* PASSWORD_CHANGED
* USER_CREATED
* USER_UPDATED
* USER_DISABLED
* ROLE_GRANTED
* ROLE_REVOKED

---

## Эксплуатация

* structured logs (JSON)

* маскирование секретов

* метрики:

    * login success/fail
    * refresh reuse
    * 401/403 rate

* traceId в логах

* алерты на аномалии авторизации

---

# 10. Идемпотентность и транзакционность

Login и refresh являются идемпотентными по смыслу.

Изменения следующих сущностей выполняются в транзакции:

* роли
* пароль
* статус пользователя

Правило безопасности:

```
последний ADMIN должен проверяться атомарно
```

Аудит:

* в MVP пишется в той же транзакции
* позже может быть вынесен через outbox

---

# 11. Структура проекта

Рекомендуемая структура Spring Boot модуля:

```
com.pb.auth

config
api
api.dto
domain
repository
service
security
audit
exception
mapper
```

---

# 12. Нефункциональные требования

* SLA для auth endpoints: p95 < 200ms
* rate limiting для `/auth/login`
* rate limiting для `/auth/refresh`
* CORS / CSRF политика
* миграции БД через Flyway или Liquibase
* обязательные integration tests для security flow

---

# 13. План реализации

## Этап 1 (MVP)

* каркас `auth-user-app`

* сущности и миграции:

    * user
    * role
    * user_role
    * refresh_token
    * audit_log

* endpoints:

    * `/auth/login`
    * `/auth/refresh`
    * `/auth/logout`
    * `/auth/password/change`

* API `/users`

* правило последнего ADMIN

* единый error handler

---

## Этап 2

* `auth_permission`
* `role_permission`
* LOCKED механизм
* защита от brute force
* управление сессиями и устройствами

---

## Этап 3

* MFA / 2FA
* SSO / OAuth2 / OIDC
* password history
* step-up authentication

```