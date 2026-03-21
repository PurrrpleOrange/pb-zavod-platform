insert into scheduling.zone (zone_id, code, name, type, capacity_companies, is_paid, is_active) values
                                                                                                    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'REST2', 'Зона отдыха 2', 'REST', 1, true, true),
                                                                                                    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'REST3', 'Зона отдыха 3', 'REST', 1, true, true)
    on conflict (zone_id) do nothing;

update scheduling.zone
set name = 'Зона отдыха 1'
where zone_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
