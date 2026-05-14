-- liquibase formatted sql

-- changeset humaNukr:08-add-parent-message-id
ALTER TABLE messages
    ADD COLUMN parent_message_id BIGINT;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_parent
        FOREIGN KEY (parent_message_id)
            REFERENCES messages (id);

-- rollback ALTER TABLE messages DROP CONSTRAINT fk_messages_parent;
-- rollback ALTER TABLE messages DROP COLUMN parent_message_id;