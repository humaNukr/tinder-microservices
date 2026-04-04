-- liquibase formatted sql

-- changeset humaNukr:02-create-chats-table
CREATE TABLE chats
(
    id         UUID PRIMARY KEY,
    user1_id   UUID                     NOT NULL,
    user2_id   UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_users UNIQUE (user1_id, user2_id),
    CONSTRAINT check_id CHECK ( user1_id < user2_id )
);
-- rollback DROP TABLE chats;