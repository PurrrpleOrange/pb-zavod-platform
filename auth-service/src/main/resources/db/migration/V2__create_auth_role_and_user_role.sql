CREATE TABLE auth_role (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_auth_role_code UNIQUE (code)
);

CREATE TABLE auth_user_role (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES auth_user (id) ON DELETE CASCADE,
    role_id     UUID        NOT NULL REFERENCES auth_role (id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID,

    CONSTRAINT uq_auth_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_auth_user_role_user_id ON auth_user_role (user_id);
CREATE INDEX idx_auth_user_role_role_id ON auth_user_role (role_id);
