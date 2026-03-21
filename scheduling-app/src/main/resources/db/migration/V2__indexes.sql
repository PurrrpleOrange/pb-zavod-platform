SET search_path TO scheduling;

-- =========================
-- GAME SLOT INDEXES
-- =========================

CREATE INDEX IF NOT EXISTS idx_game_slot_arena_start
    ON game_slot (arena_id, start_time);

CREATE INDEX IF NOT EXISTS idx_game_slot_status
    ON game_slot (status);

-- =========================
-- SLOT RESERVATION INDEXES
-- =========================

-- Для быстрого подсчёта активных (CONFIRMED + HOLD с expires_at)
CREATE INDEX IF NOT EXISTS idx_slot_reservation_active
    ON slot_reservation (game_slot_id, status, expires_at);

CREATE INDEX IF NOT EXISTS idx_slot_reservation_booking
    ON slot_reservation (booking_id);

-- =========================
-- ZONE RESERVATION INDEXES
-- =========================

CREATE INDEX IF NOT EXISTS idx_zone_res_zone_time
    ON zone_reservation (zone_id, start_time, end_time);

CREATE INDEX IF NOT EXISTS idx_zone_res_status_time
    ON zone_reservation (zone_id, status, start_time, end_time);

CREATE INDEX IF NOT EXISTS idx_zone_res_booking
    ON zone_reservation (booking_id);

CREATE INDEX IF NOT EXISTS idx_zone_res_expires
    ON zone_reservation (zone_id, status, expires_at);
