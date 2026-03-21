# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Module-Specific Commands

```bash
# Run all tests for this module
mvn -pl scheduling-app test

# Run a single test class
mvn -pl scheduling-app test -Dtest=SlotServiceTest

# Run a single test method
mvn -pl scheduling-app test -Dtest=SlotServiceTest#holdSlot_whenSlotFull_throwsException
```

## Domain Model

Five JPA entities in `domain/entity/`, all using UUID PKs:

| Entity | Purpose |
|--------|---------|
| `Arena` | Reference catalog of physical venues |
| `GameSlot` | Time interval on an arena with `capacityCompanies` limit |
| `SlotReservation` | One booking's seat in a GameSlot |
| `Zone` | Reference catalog of zones (REST / DRESSING types) |
| `ZoneReservation` | Time-interval reservation of a Zone, supports extension chains |

## State Machines

**SlotReservation:** `HOLD → CONFIRMED` or `HOLD → CANCELLED`
- HOLD is created with `expiresAt = now + holdMinutes`
- Confirm checks `expiresAt > now`; cancel is idempotent

**ZoneReservation:** `HOLD → ACTIVE → FINISHED` or `→ CANCELLED`
- Zone hold duration is derived from `GameSlot.startTime + defaultDurationMinutesForZones` (config), not from the request
- Extensions create **new records** (not updates) with `parentReservationId` pointing to the base — finishChain marks all records with same `(bookingId, zoneId)` as FINISHED

## Concurrency Pattern

All capacity-check endpoints follow the same pattern:
1. Acquire `PESSIMISTIC_WRITE` lock on the resource row (`findByIdForUpdate`)
2. Count active reservations (`CONFIRMED` + non-expired `HOLD` where `expiresAt > now`)
3. Compare against `capacityCompanies`
4. Mutate if safe

Zone confirm **re-checks overlaps** after acquiring the lock (catches races between hold and confirm).

Active overlap query (`ZoneReservationRepository.countOverlaps`) accepts an `excludeId` parameter — used during confirm to exclude self.

## Configuration Properties

`pb.scheduling.*` bound via `SchedulingProperties` record:

| Property | Default | Meaning |
|----------|---------|---------|
| `hold-minutes` | 15 | TTL for HOLD state |
| `default-duration-minutes_for_zones` | 180 | Zone reservation length from slot start |
| `hold-cleanup-interval-ms` | 60000 | How often `HoldCleanupJob` runs |

## API Surface

**Slot endpoints** (`SlotController`):
- `POST /slots/{slotId}/holds` — create HOLD → returns `{slotReservationId, expiresAt}`
- `POST /slot-holds/{id}/confirm` — HOLD → CONFIRMED
- `POST /slot-holds/{id}/cancel` — → CANCELLED (idempotent)

**Zone CRUD** (`ZoneController`): `POST/GET/PATCH/DELETE /zones`

**Zone reservation endpoints**:
- `POST /zones/{zoneId}/holds` — requires `{bookingId, slotReservationId}` in body
- `POST /zone-holds/{id}/confirm`
- `POST /zone-holds/{id}/cancel`
- `POST /zone-reservations/{id}/extend` — requires `{bookingId, extendMinutes}`
- `POST /zone-reservations/{id}/finish-chain` — marks entire extension chain FINISHED

## Test Patterns

Tests live in `src/test/java/com/pb/scheduling/service/`. All tests use **Mockito mocks** (no Spring context, no DB). Key patterns:
- `SchedulingProperties` is constructed directly with `new SchedulingProperties(15, 180)`
- `ArgumentCaptor<SlotReservation>` / `ArgumentCaptor<ZoneReservation>` used to assert on saved entity state
- Repository `findByIdForUpdate` variants are mocked alongside `findByIdAndBookingId`

## Database

Schema: `scheduling` (all tables live under this Postgres schema).
Flyway migrations: `src/main/resources/db/migration/V0__` through `V7__`.

All timestamps are `TIMESTAMPTZ`, Hibernate is configured `time_zone: UTC`. Domain code always uses `OffsetDateTime`.

**Capacity constraint rule (SQL level):** `(status <> 'HOLD') OR (expires_at IS NOT NULL)` — HOLD must always have an expiry.
