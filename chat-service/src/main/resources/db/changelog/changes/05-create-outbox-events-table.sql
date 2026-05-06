-- liquibase formatted sql

-- changeset humaNukr:05-create-outbox-events-table
CREATE TABLE outbox_events
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic      VARCHAR                                   NOT NULL,
    payload    JSONB                                     NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW() NOT NULL,
    is_sent    BOOLEAN                     DEFAULT FALSE NOT NULL
);
CREATE INDEX idx_outbox_unprocessed ON outbox_events (created_at) WHERE is_sent = false;

-- rollback DROP TABLE outbox_events;
-- rollback DROP INDEX idx_outbox_unprocessed;
