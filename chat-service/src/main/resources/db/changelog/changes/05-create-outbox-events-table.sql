--liquibase formatted sql

-- changeset humaNukr:05-create-outbox-events-table
CREATE TABLE outbox_events
(
    id           UUID PRIMARY KEY,
    topic        VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    is_processed BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_events_topic ON outbox_events (topic);
-- rollback DROP INDEX idx_outbox_events_topic;
-- rollback DROP TABLE outbox_events;
