SET search_path TO catalog;
ALTER TABLE tariff_included_item DROP CONSTRAINT IF EXISTS chk_included_has_qty;
