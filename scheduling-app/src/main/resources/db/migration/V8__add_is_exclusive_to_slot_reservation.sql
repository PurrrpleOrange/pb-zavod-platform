ALTER TABLE scheduling.slot_reservation
    ADD COLUMN is_exclusive BOOLEAN NOT NULL DEFAULT FALSE;
