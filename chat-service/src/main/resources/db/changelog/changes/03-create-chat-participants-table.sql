-- liquibase formatted sql

-- changeset humaNukr:03-create-chat-participants-table
CREATE TABLE chat_participants
(
    chat_id              UUID,
    user_id              UUID NOT NULL,
    last_read_message_id BIGINT DEFAULT (0),
    PRIMARY KEY (chat_id, user_id),
    CONSTRAINT fk_chat_participants_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE
);
-- rollback DROP TABLE chat_participants;