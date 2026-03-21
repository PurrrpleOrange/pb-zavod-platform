CREATE TABLE auth_user (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    login                 VARCHAR(100) NOT NULL,
    email                 VARCHAR(255),
    phone                 VARCHAR(20),
    password_hash         VARCHAR(255) NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,
    version               INT          NOT NULL DEFAULT 0,

    CONSTRAINT uq_auth_user_login UNIQUE (login),
    CONSTRAINT uq_auth_user_email UNIQUE (email),
    CONSTRAINT chk_auth_user_status CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED'))
);

CREATE INDEX idx_auth_user_status ON auth_user (status);
