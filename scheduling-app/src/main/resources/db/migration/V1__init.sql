SET search_path TO scheduling;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- TABLE: arena
-- =========================
CREATE TABLE IF NOT EXISTS arena (
                                     arena_id  UUID PRIMARY KEY,
                                     name      TEXT NOT NULL,
                                     is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================
-- TABLE: game_slot
-- =========================
CREATE TABLE IF NOT EXISTS game_slot (
                                         game_slot_id       UUID PRIMARY KEY,
                                         arena_id           UUID NOT NULL REFERENCES arena(arena_id),

    start_time         TIMESTAMPTZ NOT NULL,
    end_time           TIMESTAMPTZ NOT NULL,

    capacity_companies INT NOT NULL,

    -- TEXT instead of PG enum
    status             TEXT NOT NULL DEFAULT 'OPEN',

    CONSTRAINT chk_game_slot_time CHECK (end_time > start_time),
    CONSTRAINT chk_game_slot_capacity CHECK (capacity_companies > 0),
    CONSTRAINT chk_game_slot_status CHECK (status IN ('OPEN','CLOSED','CANCELLED'))
    );

-- =========================
-- TABLE: slot_reservation
-- =========================
CREATE TABLE IF NOT EXISTS slot_reservation (
                                                slot_reservation_id UUID PRIMARY KEY,
                                                game_slot_id        UUID NOT NULL REFERENCES game_slot(game_slot_id),

    booking_id          UUID NOT NULL,

    -- TEXT instead of PG enum
    status              TEXT NOT NULL,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NULL,

    CONSTRAINT chk_slot_res_status CHECK (status IN ('HOLD','CONFIRMED','CANCELLED')),
    CONSTRAINT chk_slot_hold_expires CHECK (
(status <> 'HOLD') OR (expires_at IS NOT NULL)
    )
    );

-- =========================
-- TABLE: zone
-- =========================
CREATE TABLE IF NOT EXISTS zone (
                                    zone_id            UUID PRIMARY KEY,
                                    code               TEXT NOT NULL UNIQUE,
                                    name               TEXT NOT NULL,

    -- TEXT instead of PG enum
                                    type               TEXT NOT NULL,

                                    capacity_companies INT NOT NULL,
                                    is_paid            BOOLEAN NOT NULL DEFAULT TRUE,
                                    is_active          BOOLEAN NOT NULL DEFAULT TRUE,

                                    CONSTRAINT chk_zone_capacity CHECK (capacity_companies > 0),
    CONSTRAINT chk_zone_type CHECK (type IN ('REST','DRESSING'))
    );

-- =========================
-- TABLE: zone_reservation
-- =========================
CREATE TABLE IF NOT EXISTS zone_reservation (
                                                zone_reservation_id   UUID PRIMARY KEY,
                                                zone_id               UUID NOT NULL REFERENCES zone(zone_id),

    booking_id            UUID NOT NULL,

    start_time            TIMESTAMPTZ NOT NULL,
    end_time              TIMESTAMPTZ NOT NULL,

    -- TEXT instead of PG enum
    status                TEXT NOT NULL,

    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at            TIMESTAMPTZ NULL,

    parent_reservation_id UUID NULL REFERENCES zone_reservation(zone_reservation_id),

    CONSTRAINT chk_zone_time CHECK (end_time > start_time),
    CONSTRAINT chk_zone_res_status CHECK (status IN ('HOLD','ACTIVE','CANCELLED','FINISHED')),
    CONSTRAINT chk_zone_hold_expires CHECK (
(status <> 'HOLD') OR (expires_at IS NOT NULL)
    )
    );
