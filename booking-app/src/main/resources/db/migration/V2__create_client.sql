CREATE TABLE client (
    client_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)  NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_client_phone ON client (phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_client_email ON client (email) WHERE email IS NOT NULL;

ALTER TABLE booking
    ADD CONSTRAINT fk_booking_client
    FOREIGN KEY (client_id) REFERENCES client (client_id);
