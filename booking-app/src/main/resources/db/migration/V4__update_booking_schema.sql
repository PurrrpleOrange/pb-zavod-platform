-- Drop old status CHECK constraint (auto-named by PostgreSQL as booking_status_check)
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT c.conname INTO constraint_name
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE t.relname = 'booking'
      AND n.nspname = 'booking'
      AND c.contype = 'c'
      AND c.conname LIKE '%status%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE booking DROP CONSTRAINT ' || quote_ident(constraint_name);
    END IF;
END $$;

-- Add new status CHECK constraint with all statuses
ALTER TABLE booking ADD CONSTRAINT booking_status_check
    CHECK (status IN ('HOLD','PREPAID','CONFIRMED','IN_PROGRESS',
                      'COMPLETED','NO_SHOW','CANCELLED','CANCELLED_UNPAID'));

-- Make previously NOT NULL fields nullable (support Type A bookings)
ALTER TABLE booking ALTER COLUMN game_slot_id DROP NOT NULL;
ALTER TABLE booking ALTER COLUMN tariff_id DROP NOT NULL;
ALTER TABLE booking ALTER COLUMN total_price_snapshot DROP NOT NULL;

-- New columns
ALTER TABLE booking ADD COLUMN IF NOT EXISTS desired_date           DATE;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS extra_equipment_count  INT CHECK (extra_equipment_count >= 0);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS prepaid_amount         NUMERIC(12,2) CHECK (prepaid_amount >= 0);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS admin_notes            TEXT;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS hold_expires_at        TIMESTAMPTZ;

-- Partial index for expiry job
CREATE INDEX IF NOT EXISTS idx_booking_hold_expires ON booking (status, hold_expires_at)
    WHERE status = 'HOLD';
