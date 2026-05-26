-- liquibase formatted sql

-- changeset humaNukr:04-create-inbox-events-table
CREATE TABLE inbox_events
(
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
-- rollback DROP TABLE inbox_events;
