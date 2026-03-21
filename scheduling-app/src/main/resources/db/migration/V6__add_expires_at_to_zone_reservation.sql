SET search_path TO scheduling;

ALTER TABLE zone_reservation
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ NULL;
