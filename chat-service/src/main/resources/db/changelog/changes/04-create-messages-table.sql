-- liquibase formatted sql

-- changeset humaNukr:04-create-messages-table
CREATE TABLE messages
(
    id           BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    chat_id      UUID                     NOT NULL,
    sender_id    UUID                     NOT NULL,
    content_type VARCHAR(20)              NOT NULL,
    content      TEXT                     NOT NULL,
    status       VARCHAR(20)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat FOREIGN KEY (chat_id) REFERENCES chats (id)
);

CREATE INDEX idx_messages_chat_id_id ON messages (chat_id, id DESC);
CREATE UNIQUE INDEX idx_messages_pending_content
    ON messages (content)
    WHERE status = 'UPLOADING';
CREATE INDEX idx_messages_history_lookup
    ON messages (chat_id, id DESC)
    WHERE status = 'SENT';

-- rollback DROP TABLE messages;
-- rollback DROP INDEX idx_messages_pending_content;
-- rollback DROP INDEX idx_messages_chat_id_id;
-- rollback DROP INDEX idx_messages_history_lookup;