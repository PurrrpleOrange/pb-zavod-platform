# PB Zavod Platform

Бэкенд-платформа для управления пейнтбольным клубом: бронирование игровых слотов, зон отдыха, учёт клиентов и платежей.

## Архитектура

```
                               ┌──────────────┐
                               │   Клиент     │
                               └──────┬───────┘
                                      │
                               ┌──────▼───────┐
                               │   Gateway    │  :8080
                               │  (JWT auth)  │
                               └──┬──┬──┬──┬──┘
              ┌───────────────────┘  │  │  └──────────────────┐
              ▼                      │  │                      ▼
       ┌──────────┐                  │  │               ┌─────────────┐
       │   Auth   │                  │  │               │   Catalog   │
       │  :8086   │                  │  │               │   :8083     │
       └────┬─────┘                  │  │               └──────┬──────┘
            │                        │  │                      │
            ▼                        ▼  ▼                      ▼
       ┌─────────┐   ┌─────────────┐   ┌──────────┐   ┌────────────┐
       │ pb_auth  │   │ Scheduling  │   │ Booking  │   │ pb_catalog │
       └─────────┘   │   :8081     │   │  :8082   │   └────────────┘
                     └──────┬──────┘   └───┬──┬───┘
                            │              │  │
                            ▼              ▼  │ HTTP
                     ┌─────────────┐  ┌───────┘
                     │pb_scheduling│  │
                     └─────────────┘  │
                                      ▼
                                ┌──────────┐
                                │pb_booking │
                                └──────────┘
                         ┌─── PostgreSQL 16 ───┐
```

Каждый сервис владеет своей базой данных. Межсервисное взаимодействие — синхронный HTTP (booking-app вызывает scheduling-app через WebFlux-клиент). Gateway выполняет JWT-валидацию и маршрутизацию.

## Стек технологий

| Категория | Технология |
|-----------|------------|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.2.5 |
| API Gateway | Spring Cloud Gateway 2023.0.1 |
| Безопасность | Spring Security + JJWT 0.12.6 |
| ORM | Spring Data JPA + Hibernate |
| База данных | PostgreSQL 16 |
| Миграции | Flyway |
| Документация API | SpringDoc OpenAPI 2.5.0 |
| Контейнеризация | Docker + Docker Compose |
| Тестирование | JUnit 5 + Mockito |

## Модули

| Модуль | Описание | Порт | Статус |
|--------|----------|------|--------|
| `gateway-app` | API Gateway — маршрутизация и JWT-аутентификация | 8080 | MVP |
| `auth-service` | Аутентификация, RBAC (ADMIN / INSTRUCTOR / TRAINEE) | 8086 | MVP |
| `scheduling-app` | Управление игровыми слотами и зонами, резервации | 8081 | MVP |
| `booking-app` | Бронирования, управление клиентами, Saga-оркестрация | 8082 | MVP |
| `catalog-app` | Каталог игр, тарифы, товары | 8083 | MVP |
| `sales-app` | Платежи, чеки, отчётность | — | В разработке |
| `common` | Shared-библиотека: DTO, валидация | — | Готово |

## Быстрый старт

### Docker Compose (все сервисы)

```bash
docker compose up -d
```

Платформа доступна на `http://localhost:8080` (Gateway).

### Локальная разработка

```bash
# 1. Поднять PostgreSQL
docker compose up -d postgres

# 2. Собрать проект
mvn clean install

# 3. Запустить сервисы (в порядке зависимостей)
mvn -pl auth-service spring-boot:run
mvn -pl scheduling-app spring-boot:run
mvn -pl booking-app spring-boot:run
mvn -pl gateway-app spring-boot:run
```

**Swagger UI** доступен на каждом сервисе: `http://localhost:{port}/swagger-ui`

## Ключевые решения

- **Database-per-service** — каждый сервис имеет изолированную БД и Flyway-миграции
- **Пессимистическая блокировка** (`PESSIMISTIC_WRITE`) при проверке вместимости слотов и зон для защиты от race conditions
- **Машины состояний** для резерваций: `HOLD → CONFIRMED → CANCELLED` (слоты), `HOLD → ACTIVE → FINISHED` (зоны)
- **Hold-механизм** — резервация создаётся как HOLD с TTL (15 мин), фоновый job чистит просроченные каждые 60 сек
- **Цепочки продлений** — продление зоны создаёт новую запись с `parentReservationId`, а не мутирует существующую

## API (основные эндпоинты)

### Auth (`/auth/api`)

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/auth/login` | Вход (access + refresh токены) |
| POST | `/auth/refresh` | Обновление access-токена |
| POST | `/users` | Создание пользователя (ADMIN) |
| GET | `/users` | Список пользователей (ADMIN) |

### Scheduling (`/scheduling`)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/slots/getAll` | Все игровые слоты |
| POST | `/slots/{id}/holds` | Создать HOLD на слот |
| POST | `/slot-holds/{id}/confirm` | Подтвердить резервацию |
| POST | `/zones` | Создать зону |
| POST | `/zones/{id}/holds` | Создать HOLD на зону |
| POST | `/zone-reservations/{id}/extend` | Продлить зону |

### Booking (`/booking/api`)

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/bookings` | Создать бронирование (Saga: hold слот + зона) |
| POST | `/bookings/{id}/confirm` | Подтвердить бронирование |
| POST | `/bookings/{id}/cancel` | Отменить бронирование |
| PATCH | `/bookings/{id}` | Обновить бронирование |
| POST | `/clients` | Создать клиента |
| GET | `/clients` | Список клиентов |

## Структура проекта

```
pb-zavod-platform/
├── gateway-app/          # API Gateway (Spring Cloud Gateway)
├── auth-service/         # Аутентификация и авторизация
├── scheduling-app/       # Расписание и резервации
├── booking-app/          # Бронирования и клиенты
├── catalog-app/          # Каталог игр, тарифы, товары
├── sales-app/            # Продажи (в разработке)
├── common/               # Shared DTO и валидация
├── docker/               # SQL-скрипты инициализации
├── docs/                 # Спецификации сервисов
├── docker-compose.yml
└── pom.xml               # Родительский POM
```

Каждый сервис следует единой слоёной структуре:

```
com.pb.<service>
├── api/           # REST-контроллеры
│   ├── controller/
│   ├── dto/       # Request/Response DTO
│   └── advice/    # GlobalExceptionHandler
├── service/       # Бизнес-логика
├── repository/    # Spring Data JPA
├── domain/        # JPA-сущности
├── config/        # Конфигурация
└── exception/     # Кастомные исключения
```
