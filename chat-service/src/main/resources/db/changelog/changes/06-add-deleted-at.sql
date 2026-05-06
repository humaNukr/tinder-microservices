-- liquibase formatted sql

-- changeset humaNukr:06-add-deleted-at-to-messages
ALTER TABLE messages
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
-- rollback ALTER TABLE messages DROP COLUMN deleted_at;