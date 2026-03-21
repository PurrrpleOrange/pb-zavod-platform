INSERT INTO auth_role (code, name, description, is_system)
VALUES
    ('ADMIN',      'Администратор', 'Полный доступ к системе',            TRUE),
    ('INSTRUCTOR', 'Инструктор',    'Доступ к расписанию и бронированиям', TRUE),
    ('TRAINEE',    'Стажёр',        'Ограниченный доступ',                TRUE);
