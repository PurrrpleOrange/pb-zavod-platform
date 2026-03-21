SET search_path TO catalog;
ALTER TABLE product ADD COLUMN IF NOT EXISTS subcategory TEXT;
ALTER TABLE product ADD CONSTRAINT chk_product_subcategory
    CHECK (subcategory IS NULL OR subcategory IN (
        'MARKER','BALL','AIRSOFT_GUN','LASERTAG_TAGGER',
        'EQUIPMENT','GRENADE','DRINK','SERVICE'
    ));
