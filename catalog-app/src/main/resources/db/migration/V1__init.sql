SET search_path TO catalog;

-- =========================
-- TABLE: game_type
-- =========================
CREATE TABLE IF NOT EXISTS game_type (
    game_type_id UUID PRIMARY KEY,
    name         TEXT NOT NULL,
    code         TEXT NOT NULL UNIQUE
);

-- =========================
-- TABLE: product
-- =========================
CREATE TABLE IF NOT EXISTS product (
    product_id    UUID PRIMARY KEY,
    code          TEXT    NOT NULL UNIQUE,
    name          TEXT    NOT NULL,
    category      TEXT    NOT NULL,
    unit          TEXT    NOT NULL,
    current_price NUMERIC NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_product_price CHECK (current_price >= 0),
    CONSTRAINT chk_product_category CHECK (category IN ('RENTAL','CONSUMABLE','SERVICE','DRINK')),
    CONSTRAINT chk_product_unit CHECK (unit IN ('PIECE','PACK','SET','HOUR'))
);

-- =========================
-- TABLE: tariff
-- =========================
CREATE TABLE IF NOT EXISTS tariff (
    tariff_id                    UUID PRIMARY KEY,
    game_type_id                 UUID    NOT NULL REFERENCES game_type(game_type_id),
    name                         TEXT    NOT NULL,
    price_per_player             NUMERIC NOT NULL,
    included_balls_qty_per_player INT,
    ball_caliber                 TEXT    NOT NULL,
    is_active                    BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_tariff_price CHECK (price_per_player >= 0),
    CONSTRAINT chk_tariff_caliber CHECK (ball_caliber IN ('CALIBER_68','CALIBER_50','CALIBER_43','CALIBER_12'))
);

-- =========================
-- TABLE: tariff_included_item
-- =========================
CREATE TABLE IF NOT EXISTS tariff_included_item (
    tariff_included_item_id UUID PRIMARY KEY,
    tariff_id               UUID NOT NULL REFERENCES tariff(tariff_id),
    product_id              UUID NOT NULL REFERENCES product(product_id),
    quantity_per_player     INT,
    quantity_fixed          INT,

    CONSTRAINT uq_tariff_included_product UNIQUE (tariff_id, product_id),
    CONSTRAINT chk_included_has_qty CHECK (quantity_per_player IS NOT NULL OR quantity_fixed IS NOT NULL),
    CONSTRAINT chk_included_qty_per_player CHECK (quantity_per_player IS NULL OR quantity_per_player >= 0),
    CONSTRAINT chk_included_qty_fixed CHECK (quantity_fixed IS NULL OR quantity_fixed >= 0)
);

-- =========================
-- TABLE: tariff_addon
-- =========================
CREATE TABLE IF NOT EXISTS tariff_addon (
    tariff_addon_id     UUID PRIMARY KEY,
    tariff_id           UUID    NOT NULL REFERENCES tariff(tariff_id),
    product_id          UUID    NOT NULL REFERENCES product(product_id),
    price               NUMERIC NOT NULL,
    max_qty_per_player  INT,
    max_qty_per_booking INT,

    CONSTRAINT uq_tariff_addon_product UNIQUE (tariff_id, product_id),
    CONSTRAINT chk_addon_price CHECK (price >= 0),
    CONSTRAINT chk_addon_qty_player CHECK (max_qty_per_player IS NULL OR max_qty_per_player >= 0),
    CONSTRAINT chk_addon_qty_booking CHECK (max_qty_per_booking IS NULL OR max_qty_per_booking >= 0)
);
