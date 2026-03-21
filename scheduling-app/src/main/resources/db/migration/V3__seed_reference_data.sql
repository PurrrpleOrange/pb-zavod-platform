SET search_path TO scheduling;

-- =========================
-- ARENAS
-- =========================

INSERT INTO arena (arena_id, name, is_active) VALUES
                                                  ('11111111-1111-1111-1111-111111111111', 'gamefield1', TRUE),
                                                  ('22222222-2222-2222-2222-222222222222', 'gamefield2', TRUE),
                                                  ('33333333-3333-3333-3333-333333333333', 'gamefield3', TRUE)
    ON CONFLICT (arena_id) DO NOTHING;

-- =========================
-- ZONES
-- =========================

INSERT INTO zone (zone_id, code, name, type, capacity_companies, is_paid, is_active) VALUES
                                                                                         ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'DRESSING', 'Раздевалка', 'DRESSING', 2, FALSE, TRUE),
                                                                                         ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'REST1', 'Зона отдыха 1', 'REST', 1, TRUE, TRUE),
                                                                                         ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'REST2', 'Зона отдыха 2', 'REST', 1, TRUE, TRUE),
                                                                                         ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'REST3', 'Зона отдыха 3', 'REST', 1, TRUE, TRUE)
    ON CONFLICT (zone_id) DO NOTHING;
