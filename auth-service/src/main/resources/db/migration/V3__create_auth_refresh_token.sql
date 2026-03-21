CREATE TABLE auth_refresh_token (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES auth_user (id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    jti             VARCHAR(100) NOT NULL,
    issued_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by_jti VARCHAR(100),
    device_id       VARCHAR(255),
    ip              VARCHAR(45),
    user_agent      VARCHAR(500),

    CONSTRAINT uq_auth_refresh_token_jti UNIQUE (jti)
);

CREATE INDEX idx_auth_refresh_token_user_id    ON auth_refresh_token (user_id);
CREATE INDEX idx_auth_refresh_token_expires_at  ON auth_refresh_token (expires_at);
