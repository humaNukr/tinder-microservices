-- liquibase formatted sql

-- changeset humaNukr:04-create-messages-table
CREATE TABLE messages
(
    id           BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    chat_id      UUID                     NOT NULL,
    sender_id    UUID                     NOT NULL,
    content_type VARCHAR(20)              NOT NULL,
    content      TEXT                     NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_chat FOREIGN KEY (chat_id) REFERENCES chats (id)
);

CREATE INDEX idx_messages_chat_id_id ON messages (chat_id, id DESC);
-- rollback DROP TABLE messages;