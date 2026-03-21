CREATE TABLE booking (
    booking_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id        UUID         NOT NULL,
    game_slot_id     UUID         NOT NULL,
    tariff_id        UUID         NOT NULL,
    players_count    INT          NOT NULL CHECK (players_count > 0),
    total_price_snapshot NUMERIC(12,2) NOT NULL CHECK (total_price_snapshot >= 0),
    status           VARCHAR(20)  NOT NULL DEFAULT 'HOLD'
                     CHECK (status IN ('HOLD', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    slot_reservation_id  UUID,
    zone_reservation_id  UUID,
    cancel_reason    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       UUID,
    updated_by       UUID,
    version          INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_booking_client_id    ON booking (client_id);
CREATE INDEX idx_booking_game_slot_id ON booking (game_slot_id);
CREATE INDEX idx_booking_status       ON booking (status);
CREATE INDEX idx_booking_created_at   ON booking (created_at);
