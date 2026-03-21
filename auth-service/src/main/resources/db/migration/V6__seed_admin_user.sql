-- Seed initial admin user
-- login: admin, password: admin123
INSERT INTO auth.auth_user (id, login, email, password_hash, status, failed_login_attempts, created_at, updated_at, version)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@pb-zavod.ru',
    '$2a$10$aqsAc4I3bW.IMHVqj8QUJOmXW//yvct4LLatQNLLFFMhx/iPswQKe',
    'ACTIVE',
    0,
    now(),
    now(),
    0
)
ON CONFLICT (login) DO NOTHING;

-- Assign ADMIN role
INSERT INTO auth.auth_user_role (id, user_id, role_id, assigned_at)
SELECT gen_random_uuid(), u.id, r.id, now()
FROM auth.auth_user u
JOIN auth.auth_role r ON r.code = 'ADMIN'
WHERE u.login = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;
