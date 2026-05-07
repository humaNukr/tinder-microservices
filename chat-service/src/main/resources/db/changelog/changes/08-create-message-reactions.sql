-- liquibase formatted sql

-- changeset humaNukr:09-create-message-reactions
CREATE TABLE message_reactions
(
    id         BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    message_id BIGINT                   NOT NULL,
    user_id    UUID                     NOT NULL,
    reaction   VARCHAR(50)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT uq_message_user_reaction UNIQUE (message_id, user_id)
);

-- rollback DROP TABLE message_reactions;