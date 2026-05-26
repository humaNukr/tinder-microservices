-- liquibase formatted sql

-- changeset swipe-service:03-create-shedlock-table
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
-- rollback DROP TABLE shedlock;
