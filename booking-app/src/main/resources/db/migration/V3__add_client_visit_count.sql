ALTER TABLE client
    ADD COLUMN visit_count INT NOT NULL DEFAULT 0 CHECK (visit_count >= 0);
