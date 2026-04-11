-- liquibase formatted sql

-- changeset humaNukr:02-create-notifications-table
CREATE TABLE notifications
(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    metadata   JSONB,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id_created_at
    ON notifications (user_id, created_at DESC);

CREATE INDEX idx_notifications_unread
    ON notifications (user_id) WHERE is_read = FALSE;
-- rollback DROP INDEX idx_notifications_user_id_created_at;
-- rollback DROP INDEX idx_notifications_unread;
-- rollback DROP TABLE notifications;