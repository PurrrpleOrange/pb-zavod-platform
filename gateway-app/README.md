# gateway-app

Единая точка входа для всей платформы pb-zavod. Реализован на **Spring Cloud Gateway** (реактивный стек, WebFlux).

**Порт:** `8080`

---

## Зачем нужен gateway

- **Централизованная JWT-валидация** — scheduling-app и booking-app не имеют Spring Security; вместо того чтобы добавлять JWT-фильтр в каждый сервис, вся проверка токенов происходит здесь
- **Единый endpoint для клиентов** — клиент работает только с `localhost:8080`, не зная о внутренних портах сервисов
- **Проброс контекста** — после успешной проверки токена gateway добавляет заголовки `X-User-Id`, `X-User-Roles`, `X-Trace-Id` во все downstream-запросы

---

## Архитектура

```
Клиент
  │
  ▼ :8080
┌──────────────────────────────────────────────┐
│               gateway-app                   │
│                                              │
│  JwtAuthenticationFilter (order=-100)        │
│  ├─ публичные пути → пропустить              │
│  ├─ нет токена    → 401 TOKEN_MISSING        │
│  ├─ плохой токен  → 401 TOKEN_INVALID        │
│  └─ ок → добавить X-User-Id, X-User-Roles   │
│                                              │
│  Routing (RewritePath)                       │
│  /auth/**       → auth-service:8086         │
│  /scheduling/** → scheduling-app:8081       │
│  /booking/**    → booking-app:8082          │
│  /catalog/**    → catalog-app:8083          │
└──────────────────────────────────────────────┘

Inter-service (минуя gateway):
  booking-app → scheduling-app напрямую (http://scheduling-app:8081)
```

---

## Маршруты

Gateway добавляет префикс для маршрутизации и снимает его перед форвардом (`RewritePath`). Существующие сервисы не изменялись.

| Клиент вызывает (gateway :8080) | Downstream получает |
|---|---|
| `POST /auth/api/auth/login` | `POST /api/auth/login` → auth-service:8086 |
| `POST /auth/api/auth/refresh` | `POST /api/auth/refresh` → auth-service:8086 |
| `POST /auth/api/auth/logout` | `POST /api/auth/logout` → auth-service:8086 |
| `GET  /auth/api/users` | `GET /api/users` → auth-service:8086 |
| `GET  /scheduling/slots/getAll` | `GET /slots/getAll` → scheduling-app:8081 |
| `POST /scheduling/zones/{id}/holds` | `POST /zones/{id}/holds` → scheduling-app:8081 |
| `POST /scheduling/slot-holds/{id}/confirm` | `POST /slot-holds/{id}/confirm` → scheduling-app:8081 |
| `POST /booking/api/bookings` | `POST /api/bookings` → booking-app:8082 |
| `GET  /booking/api/clients/{id}` | `GET /api/clients/{id}` → booking-app:8082 |
| `GET  /catalog/game-types` | `GET /game-types` → catalog-app:8083 |

### Публичные маршруты (JWT не требуется)

| Путь | Описание |
|---|---|
| `POST /auth/api/auth/login` | Получить токены |
| `POST /auth/api/auth/refresh` | Обновить access token |
| `/swagger-ui/**` | Swagger UI |
| `/api-docs/**` | OpenAPI spec |
| `/actuator/**` | Health/metrics |

Все остальные маршруты требуют заголовок `Authorization: Bearer <token>`.

---

## JWT-валидация

Gateway использует тот же алгоритм (HMAC-SHA), что и `auth-service`:
- Ключ: `Keys.hmacShaKeyFor(jwtSecret.getBytes(UTF_8))`
- Библиотека: JJWT 0.12.6

**Важно:** `pb.gateway.jwt-secret` должен совпадать с `auth.jwt.access-secret` в auth-service. В production оба значения задаются через переменные окружения.

### Заголовки, добавляемые в downstream-запросы

| Заголовок | Значение |
|---|---|
| `X-User-Id` | UUID пользователя (subject из токена) |
| `X-User-Roles` | Роли через запятую (`ADMIN,INSTRUCTOR`) |
| `X-Trace-Id` | UUID запроса (из входящего запроса или сгенерированный) |

### Формат ошибок

```json
{
  "code": "GATEWAY_TOKEN_MISSING",
  "message": "Authorization header is required",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-26T10:15:30Z"
}
```

Коды ошибок:
- `GATEWAY_TOKEN_MISSING` — нет заголовка `Authorization: Bearer ...`
- `GATEWAY_TOKEN_INVALID` — токен не прошёл верификацию или истёк

---

## Конфигурация

`src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://auth-service:8086
          predicates: [Path=/auth/**]
          filters: [RewritePath=/auth/(?<remaining>.*), /${remaining}]
        # ... остальные маршруты

pb:
  gateway:
    jwt-secret: "..."  # должен совпадать с auth.jwt.access-secret в auth-service
```

### Переменные окружения

| Переменная | Описание |
|---|---|
| `PB_GATEWAY_JWT_SECRET` | JWT-секрет (обязательно в production) |
| `SPRING_CLOUD_GATEWAY_ROUTES_0_URI` | Override URI для auth-service (опционально) |

---

## Запуск

### Локально (без Docker)

```bash
# gateway обращается к сервисам по именам контейнеров — нужно переопределить URI
# либо запустить все сервисы локально и изменить application.yml (localhost:...)

mvn -pl gateway-app spring-boot:run
```

### Через Docker Compose

```bash
# Запустить весь стек
docker compose up -d

# Только gateway + зависимости
docker compose up -d postgres auth-service scheduling-app booking-app gateway-app
```

---

## Проверка работы

```bash
# 1. Логин — публичный эндпоинт (токен не нужен)
curl -X POST http://localhost:8080/auth/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin123"}'
# → {"accessToken":"...","refreshToken":"..."}

# 2. Запрос без токена → 401
curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8080/booking/api/bookings
# → 401

# 3. Запрос с токеном
export TOKEN="<accessToken из шага 1>"

curl http://localhost:8080/scheduling/slots/getAll \
  -H "Authorization: Bearer $TOKEN"
# → 200 [...список слотов...]

curl http://localhost:8080/booking/api/bookings \
  -H "Authorization: Bearer $TOKEN"
# → 200 [...список бронирований...]

# 4. Истёкший/невалидный токен → 401
curl http://localhost:8080/scheduling/slots/getAll \
  -H "Authorization: Bearer invalid.token.here"
# → {"code":"GATEWAY_TOKEN_INVALID","message":"Token is invalid or expired",...}
```

---

## Структура модуля

```
gateway-app/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/pb/gateway/
    │   ├── GatewayApplication.java
    │   ├── config/
    │   │   └── GatewayProperties.java        — @ConfigurationProperties("pb.gateway")
    │   ├── security/
    │   │   └── GatewayJwtProvider.java        — validateToken, parseClaims
    │   ├── filter/
    │   │   └── JwtAuthenticationFilter.java   — GlobalFilter, Ordered(-100)
    │   └── exception/
    │       └── GatewayErrorResponse.java      — формат ошибок {code, message, traceId, timestamp}
    └── resources/
        └── application.yml
```

---

## Известные ограничения

- **Нет service discovery** — URI сервисов заданы статически. При изменении портов нужно обновить `application.yml`
- **Нет rate limiting** — планируется добавить через `RequestRateLimiter` filter
- **Нет circuit breaker** — планируется Resilience4j
- **catalog-app** — маршрут настроен, но сервис ещё не добавлен в `docker-compose.yml` (нет Dockerfile); запросы к `/catalog/**` вернут 502 до появления сервиса
