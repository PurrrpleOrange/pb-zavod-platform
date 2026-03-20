# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Module Context

This is the `booking-app` module (port 8082, database `pb_booking`, schema `booking`). See the root `CLAUDE.md` for project-wide conventions, build commands, and architecture overview. Booking-app depends on `scheduling-app` being up.

## Booking State Machine

`BookingStatus` transitions:
```
HOLD → CONFIRMED → COMPLETED
                 → NO_SHOW
     → CANCELLED
CONFIRMED → CANCELLED
```

All state transitions are idempotent (repeated calls to an already-terminal state return the existing booking). Invalid transitions throw `BookingException(BOOKING_INVALID_STATE)` → HTTP 409.

Both `COMPLETED` and `NO_SHOW` release scheduling resources (slot/zone holds) after updating the booking status. `COMPLETED` additionally increments `Client.visitCount`.

## Two-Phase Commit with Scheduling-App

`createBooking` follows a strict sequence with compensating rollbacks:

1. Save `Booking` with `status=HOLD` (flush to DB)
2. `POST /slots/{slotId}/holds` → get `slotReservationId`
3. If slot hold fails → delete booking, throw
4. If `zoneId` provided: `POST /zones/{zoneId}/holds` → get `zoneReservationId`
5. If zone hold fails → cancel slot hold, delete booking, throw
6. Save booking with both reservation IDs

`confirmBooking` calls `POST /slot-holds/{id}/confirm` and (if zone) `POST /zone-holds/{id}/confirm`.

Cancellation errors in `cancelSlotHold`/`cancelZoneHold` are logged as warnings but **not** re-thrown — the booking status update proceeds regardless.

## SchedulingClient

Located in `client/`. Uses Spring WebFlux `WebClient` but calls `.block()` synchronously — this is intentional. Error responses from scheduling-app throw `BookingException` with the appropriate `ErrorCode`. Cancel operations use `exchangeToMono` (not `retrieve`) so failures are only logged, not propagated.

Configured via `pb.booking.scheduling-base-url` in `application.yml`.

## Additional Layer: mapper/

Beyond the standard layer structure, booking-app has a `mapper/` package with `BookingMapper` and `ClientMapper` (plain Spring `@Component` beans, not MapStruct). Controllers use mappers to convert domain entities to response DTOs — services return entities, not DTOs.

## Optimistic Locking

`Booking` entity has a `@Version int version` field. This is the concurrency control for booking mutations (not pessimistic locking as used in scheduling-app).

## API Endpoints

- `POST /api/bookings` — create (body: `CreateBookingRequest`)
- `POST /api/bookings/{id}/confirm`
- `POST /api/bookings/{id}/cancel` — body optional (`CancelBookingRequest.reason`)
- `POST /api/bookings/{id}/complete`
- `POST /api/bookings/{id}/no-show`
- `GET /api/bookings/{id}`
- `GET /api/bookings?clientId=&gameSlotId=&status=` — paginated, default page size 20
- `GET /api/clients`, `POST /api/clients`, `GET /api/clients/{id}`, `PUT /api/clients/{id}`
