SET search_path TO catalog;

CREATE INDEX idx_tariff_game_type ON tariff(game_type_id);
CREATE INDEX idx_tariff_active ON tariff(is_active);
CREATE INDEX idx_product_active ON product(is_active);
CREATE INDEX idx_product_category ON product(category);
CREATE INDEX idx_tariff_included_item_tariff ON tariff_included_item(tariff_id);
CREATE INDEX idx_tariff_included_item_product ON tariff_included_item(product_id);
CREATE INDEX idx_tariff_addon_tariff ON tariff_addon(tariff_id);
CREATE INDEX idx_tariff_addon_product ON tariff_addon(product_id);
