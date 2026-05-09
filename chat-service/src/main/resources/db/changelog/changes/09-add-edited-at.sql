-- liquibase formatted sql

-- changeset humaNukr:09-add-edited-at
ALTER TABLE messages
    ADD COLUMN edited_at TIMESTAMP WITH TIME ZONE;
-- rollback ALTER TABLE messages DROP COLUMN edited_at;