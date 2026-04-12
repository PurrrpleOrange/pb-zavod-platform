# Backend Architecture

## System Overview

```mermaid
graph TB
    subgraph Clients
        WEB[Web App / SPA]
        MOB[Mobile App]
    end

    subgraph "API Gateway :8080"
        GW[gateway-app<br/>Spring Cloud Gateway]
        JWT_FILTER[JWT Auth Filter<br/>order -100]
    end

    subgraph "Backend Services"
        AUTH[auth-service<br/>:8086]
        SCHED[scheduling-app<br/>:8081]
        BOOK[booking-app<br/>:8082]
        CAT[catalog-app<br/>:8083]
        SALES[sales-app<br/>TBD]
    end

    subgraph "PostgreSQL :5433"
        DB_AUTH[(pb_auth)]
        DB_SCHED[(pb_scheduling)]
        DB_BOOK[(pb_booking)]
        DB_CAT[(pb_catalog)]
        DB_SALES[(pb_sales)]
    end

    LIB[[common<br/>shared DTOs & validation]]

    WEB & MOB -->|HTTPS| GW
    GW --> JWT_FILTER
    JWT_FILTER -->|/auth/**| AUTH
    JWT_FILTER -->|/scheduling/**| SCHED
    JWT_FILTER -->|/booking/**| BOOK
    JWT_FILTER -->|/catalog/**| CAT

    AUTH --> DB_AUTH
    SCHED --> DB_SCHED
    BOOK --> DB_BOOK
    CAT --> DB_CAT
    SALES --> DB_SALES

    BOOK -->|WebFlux HTTP| SCHED

    LIB -.->|dependency| AUTH
    LIB -.->|dependency| SCHED
    LIB -.->|dependency| BOOK
    LIB -.->|dependency| CAT

    style GW fill:#4A90D9,color:#fff
    style AUTH fill:#E8A838,color:#fff
    style SCHED fill:#50B83C,color:#fff
    style BOOK fill:#9C6ADE,color:#fff
    style CAT fill:#47C1BF,color:#fff
    style SALES fill:#999,color:#fff
    style LIB fill:#DFE3E8,color:#333
```

## Authentication Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as Gateway :8080
    participant A as Auth Service :8086
    participant S as Backend Service

    Note over C,S: Login
    C->>GW: POST /auth/api/auth/login
    GW->>A: POST /api/auth/login (public path, no JWT check)
    A-->>GW: {accessToken, refreshToken, expiresIn}
    GW-->>C: 200 OK + tokens

    Note over C,S: Authenticated Request
    C->>GW: GET /booking/api/bookings<br/>Authorization: Bearer <token>
    GW->>GW: Validate JWT signature & expiration
    GW->>S: Forward + headers:<br/>X-User-Id, X-User-Roles, X-Trace-Id
    S-->>GW: Response
    GW-->>C: Response

    Note over C,S: Token Refresh
    C->>GW: POST /auth/api/auth/refresh
    GW->>A: POST /api/auth/refresh (public path)
    A-->>GW: {new accessToken, new refreshToken}
    GW-->>C: 200 OK + new tokens
```

## Booking Two-Phase Commit (Saga)

```mermaid
sequenceDiagram
    participant C as Client
    participant B as Booking :8082
    participant S as Scheduling :8081

    Note over C,S: Create Booking (HOLD phase)
    C->>B: POST /api/bookings
    B->>S: POST /slots/{slotId}/holds
    S-->>B: {slotReservationId, holdExpiresAt}

    opt Zone requested
        B->>S: POST /zones/{zoneId}/holds
        alt Zone hold success
            S-->>B: {zoneReservationId, holdExpiresAt}
        else Zone hold fails
            S-->>B: Error
            B->>S: POST /slot-holds/{id}/cancel (compensate)
            B-->>C: 422 Error
        end
    end

    B-->>C: 201 Booking (status=HOLD)

    Note over C,S: Confirm Booking
    C->>B: POST /api/bookings/{id}/confirm
    B->>S: POST /slot-holds/{id}/confirm
    opt Has zone
        B->>S: POST /zone-holds/{id}/confirm
    end
    B-->>C: 200 Booking (status=CONFIRMED)

    Note over S,S: Background: Cleanup Job (every 60s)
    S->>S: Expire stale HOLDs older than 15min
```

## Booking State Machine

```mermaid
stateDiagram-v2
    [*] --> HOLD: submitBooking
    HOLD --> CONFIRMED: confirmBooking<br/>(requires prepayment)
    HOLD --> CANCELLED: cancelBooking
    CONFIRMED --> COMPLETED: completeBooking
    CONFIRMED --> NO_SHOW: noShowBooking
    CONFIRMED --> CANCELLED: cancelBooking
    HOLD --> [*]: auto-expire (15 min)
```

## Reservation State Machines

```mermaid
stateDiagram-v2
    state "SlotReservation" as slot {
        [*] --> s_HOLD: holdSlot
        s_HOLD --> s_CONFIRMED: confirm
        s_HOLD --> s_CANCELLED: cancel / expire
        s_CONFIRMED --> s_CANCELLED: cancel
    }

    state "ZoneReservation" as zone {
        [*] --> z_HOLD: holdZone
        z_HOLD --> z_CONFIRMED: confirm
        z_HOLD --> z_CANCELLED: cancel / expire
        z_CONFIRMED --> z_CANCELLED: cancel
        z_CONFIRMED --> z_CONFIRMED: extend (chain)
    }
```

## Service Layer Architecture (per service)

```mermaid
graph TB
    subgraph "Each Microservice"
        API[api/<br/>REST Controllers]
        SVC[service/<br/>Business Logic<br/>@Transactional]
        REPO[repository/<br/>Spring Data JPA]
        DOM[domain/<br/>Entities & Enums]
        CFG[config/<br/>Spring Configuration]
        EXC[exception/<br/>GlobalExceptionHandler]
    end

    API --> SVC
    SVC --> REPO
    REPO --> DOM
    API --> EXC
    SVC --> EXC

    style API fill:#4A90D9,color:#fff
    style SVC fill:#50B83C,color:#fff
    style REPO fill:#E8A838,color:#fff
    style DOM fill:#9C6ADE,color:#fff
    style CFG fill:#DFE3E8,color:#333
    style EXC fill:#DE3618,color:#fff
```

## Database Schema (ER Overview)

```mermaid
erDiagram
    %% Auth Service
    USER ||--o{ USER_ROLE : has
    ROLE ||--o{ USER_ROLE : assigned_to
    USER ||--o{ REFRESH_TOKEN : owns
    USER ||--o{ AUDIT_LOG : generates

    %% Scheduling Service
    GAME_SLOT ||--o{ SLOT_RESERVATION : has
    ZONE ||--o{ ZONE_RESERVATION : has

    %% Booking Service
    CLIENT ||--o{ BOOKING : makes
    BOOKING ||--|| SLOT_RESERVATION : "logical FK"
    BOOKING ||--o| ZONE_RESERVATION : "logical FK"

    %% Catalog Service
    GAME_TYPE ||--o{ TARIFF : has
    TARIFF ||--o{ TARIFF_ADDON : includes
    TARIFF ||--o{ TARIFF_INCLUDED_ITEM : bundles
    PRODUCT ||--o{ TARIFF_ADDON : used_in
    PRODUCT ||--o{ TARIFF_INCLUDED_ITEM : used_in

    USER {
        uuid id PK
        string username
        string password_hash
        string email
        enum status
    }

    GAME_SLOT {
        bigint id PK
        bigint game_type_id
        timestamp start_time
        timestamp end_time
        int available_seats
        int occupied_seats
    }

    SLOT_RESERVATION {
        bigint id PK
        bigint game_slot_id FK
        uuid booking_id
        enum status
        boolean is_exclusive
        timestamp expires_at
    }

    ZONE {
        bigint id PK
        string name
        int max_concurrent
        int current_concurrent
        boolean active
    }

    ZONE_RESERVATION {
        bigint id PK
        bigint zone_id FK
        uuid booking_id
        bigint slot_reservation_id
        enum status
        timestamp start_time
        timestamp end_time
    }

    BOOKING {
        uuid id PK
        bigint client_id FK
        bigint game_slot_id
        bigint slot_reservation_id
        bigint zone_id
        bigint zone_reservation_id
        enum status
        decimal prepayment_amount
        int version
    }

    CLIENT {
        bigint id PK
        string name
        string email
        string phone
        int visit_count
    }

    GAME_TYPE {
        bigint id PK
        string name
        int min_players
        int max_players
        boolean active
    }

    TARIFF {
        bigint id PK
        bigint game_type_id FK
        string name
        decimal price
        int duration_minutes
    }

    PRODUCT {
        bigint id PK
        string name
        enum category
        boolean active
    }
```

## Docker Compose Topology

```mermaid
graph LR
    subgraph "Docker Network"
        PG[PostgreSQL :5433<br/>pb-postgres]

        AUTH[auth-service :8086]
        SCHED[scheduling-app :8081]
        BOOK[booking-app :8082]
        GW[gateway-app :8080]
    end

    PG -->|healthcheck| AUTH
    PG -->|healthcheck| SCHED
    PG -->|healthcheck| BOOK

    SCHED -->|started| BOOK
    AUTH & SCHED & BOOK -->|started| GW

    VOL[(pb_pg_data)]
    VOL -.-> PG

    style PG fill:#336791,color:#fff
    style GW fill:#4A90D9,color:#fff
    style AUTH fill:#E8A838,color:#fff
    style SCHED fill:#50B83C,color:#fff
    style BOOK fill:#9C6ADE,color:#fff
```

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Gateway | Spring Cloud Gateway (2023.0.1) |
| Security | Spring Security + JJWT 0.12.6 |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Database | PostgreSQL 16 |
| HTTP Client | Spring WebFlux WebClient |
| API Docs | SpringDoc OpenAPI 2.5.0 |
| Testing | JUnit 5 + Mockito |
| Build | Maven (multi-module) |
| Containers | Docker (eclipse-temurin) + Docker Compose |

## Ports & Databases

| Service | Port | Database | Schema | Status |
|---------|------|----------|--------|--------|
| gateway-app | 8080 | - | - | MVP |
| auth-service | 8086 | pb_auth | auth | MVP |
| scheduling-app | 8081 | pb_scheduling | scheduling | MVP |
| booking-app | 8082 | pb_booking | booking | MVP |
| catalog-app | 8083 | pb_catalog | catalog | Skeleton |
| sales-app | TBD | pb_sales | sales | Skeleton |
