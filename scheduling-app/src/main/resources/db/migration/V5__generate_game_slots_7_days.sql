SET search_path TO scheduling;

-- Генерация game_slot на 7 дней вперёд (включая сегодня)
-- Интервалы: 09-12, 12-15, 15-18, 18-21
-- На 3 арены, capacity_companies = 2, status = OPEN
-- Таймзона: +03 (как у тебя в логах)

WITH days AS (
    SELECT (current_date + offs)::date AS d
    FROM generate_series(0, 6) AS offs
),
intervals AS (
    SELECT 9  AS h_start, 12 AS h_end UNION ALL
    SELECT 12 AS h_start, 15 AS h_end UNION ALL
    SELECT 15 AS h_start, 18 AS h_end UNION ALL
    SELECT 18 AS h_start, 21 AS h_end
),
arenas AS (
    SELECT arena_id
    FROM arena
    WHERE is_active = TRUE
),
slots AS (
    SELECT
        gen_random_uuid() AS game_slot_id,
        a.arena_id,
        -- Явно задаём +03:00, чтобы не зависеть от timezone сервера Postgres
        make_timestamptz(
            EXTRACT(YEAR  FROM d)::int,
            EXTRACT(MONTH FROM d)::int,
            EXTRACT(DAY   FROM d)::int,
            i.h_start, 0, 0,
            '+03:00'
        ) AS start_time,
        make_timestamptz(
            EXTRACT(YEAR  FROM d)::int,
            EXTRACT(MONTH FROM d)::int,
            EXTRACT(DAY   FROM d)::int,
            i.h_end, 0, 0,
            '+03:00'
        ) AS end_time
    FROM days
    CROSS JOIN intervals i
    CROSS JOIN arenas a
)
INSERT INTO game_slot (game_slot_id, arena_id, start_time, end_time, capacity_companies, status)
SELECT
    s.game_slot_id,
    s.arena_id,
    s.start_time,
    s.end_time,
    2,
    'OPEN'
FROM slots s
WHERE NOT EXISTS (
    SELECT 1
    FROM game_slot gs
    WHERE gs.arena_id = s.arena_id
      AND gs.start_time = s.start_time
      AND gs.end_time = s.end_time
);
