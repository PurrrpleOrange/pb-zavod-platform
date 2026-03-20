# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Start PostgreSQL (required before running any service)
docker compose up -d

# Build entire project
mvn clean install

# Build a single module
mvn -pl scheduling-app clean install

# Run a specific service
mvn -pl auth-service spring-boot:run      # port 8086
mvn -pl scheduling-app spring-boot:run    # port 8081
mvn -pl booking-app spring-boot:run       # port 8082

# Run all tests
mvn test

# Run tests for a single module
mvn -pl scheduling-app test
```

**Service startup order** (dependency chain): auth-service → scheduling-app → booking-app

## Architecture Overview

Maven multi-module monorepo with 6 modules:

| Module | Status | Port | DB |
|--------|--------|------|----|
| `common` | Complete | N/A | N/A |
| `auth-service` | MVP done | 8086 | pb_auth |
| `scheduling-app` | MVP done | 8081 | pb_scheduling |
| `booking-app` | In progress | 8082 | pb_booking |
| `catalog-app` | Skeleton | TBD | pb_catalog |
| `sales-app` | Skeleton | TBD | pb_sales |

**`common`** is a plain library (no Spring) providing shared DTOs and validation annotations used by other modules.

## Technology Stack

- **Java 21**, **Spring Boot 3.2.5**
- **PostgreSQL 16** on port 5433, credentials `pb/pb`
- **Flyway** for migrations (per-service `resources/db/migration/`)
- **Spring Data JPA** with Hibernate
- **Lombok** everywhere
- **SpringDoc OpenAPI 2.5.0** — Swagger UI at `/swagger-ui`, spec at `/api-docs`
- **WebFlux** (reactive HTTP client) in booking-app and sales-app for inter-service calls
- **JJWT 0.12.6** in auth-service
- **JUnit 5 + Mockito** for tests (only scheduling-app has tests currently)

## Code Architecture Patterns

**Layer structure** (consistent across services):
```
api/          — REST controllers (@RestController)
service/      — Business logic (@Service, @Transactional)
repository/   — Spring Data JPA repositories
domain/       — JPA entities and enums
config/       — Spring configuration classes
exception/    — Custom exceptions and GlobalExceptionHandler
```

**Package prefix:** `com.pb.<service-name>`

**Error handling:** Unified response via `GlobalExceptionHandler` (@ControllerAdvice). All errors return `{code, message, details}`. Business rules throw `BusinessException` (→ 422), missing resources throw `NotFoundException` (→ 404).

**Concurrency:** Pessimistic locking (`PESSIMISTIC_WRITE`) on reads before mutations for anything with capacity limits or conflict potential.

**State machines:**
- `SlotReservation`: `HOLD → CONFIRMED → CANCELLED`
- `ZoneReservation`: `HOLD → CONFIRMED → CANCELLED` (supports extension chaining)
- Scheduling-app runs a cleanup job (every 60s) to expire stale HOLDs after 15 minutes

**Inter-service communication:** Booking-app calls scheduling-app via a Spring WebFlux `SchedulingClient` (configured via `pb.booking.scheduling-base-url`). No shared databases — `bookingId` is a logical FK only.

**Database per service** — each service owns its schema exclusively. Flyway runs on startup; `ddl-auto: validate` in production-like configs.

## Specification Docs (Russian)

- `docs/specs/00-system-overview.md` — error codes, NFRs, security requirements
- `docs/specs/05-catalog-service.md` — catalog service requirements
- `docs/specs/06-sales-service.md` — sales service requirements
- `scheduling-app/BUSINESS_LOGIC_ANALYSIS.md` — detailed scheduling business logic
- `auth-service/blueprint_auth-service.md` — auth service design

## Configuration Notes

Each service has `src/main/resources/application.yml`. Service-specific properties use namespaced prefixes (e.g., `pb.scheduling.*`, `pb.booking.*`) bound via `@ConfigurationProperties`.

The JWT secret in auth-service's `application.yml` is a placeholder — externalize it via environment variable before any non-local deployment.
