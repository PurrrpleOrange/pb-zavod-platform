SET search_path TO catalog;
ALTER TABLE tariff DROP CONSTRAINT IF EXISTS chk_tariff_caliber;
ALTER TABLE tariff DROP COLUMN IF EXISTS ball_caliber;
ALTER TABLE tariff DROP COLUMN IF EXISTS included_balls_qty_per_player;
