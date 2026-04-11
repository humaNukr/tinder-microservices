-- liquibase formatted sql

-- changeset humaNukr:01-create-device-tokens-table
CREATE TABLE device_tokens
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    device_type VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
-- rollback DROP INDEX idx_device_tokens_user_id;
-- rollback DROP TABLE device_tokens;