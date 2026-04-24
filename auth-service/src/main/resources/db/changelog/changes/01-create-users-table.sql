-- liquibase formatted sql

-- changeset humaNukr:01-create-users-table
CREATE TABLE users
(
    id                UUID                     NOT NULL PRIMARY KEY,
    email             VARCHAR(255)             NOT NULL UNIQUE,
    is_email_verified BOOLEAN                  NOT NULL,
    role              VARCHAR(255)             NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
-- rollback DROP TABLE users;