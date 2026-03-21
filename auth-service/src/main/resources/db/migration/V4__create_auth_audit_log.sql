CREATE TABLE auth_audit_log (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(50)  NOT NULL,
    actor_user_id   UUID,
    target_user_id  UUID,
    entity_type     VARCHAR(50),
    entity_id       VARCHAR(100),
    result          VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    error_code      VARCHAR(50),
    metadata        JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_audit_log_created_at      ON auth_audit_log (created_at);
CREATE INDEX idx_auth_audit_log_actor_user_id   ON auth_audit_log (actor_user_id);
CREATE INDEX idx_auth_audit_log_target_user_id  ON auth_audit_log (target_user_id);
CREATE INDEX idx_auth_audit_log_event_type      ON auth_audit_log (event_type);
