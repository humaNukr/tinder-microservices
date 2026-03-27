-- liquibase formatted sql

-- changeset humaNukr:01-create-swipes-table
CREATE TABLE swipes
(
    user1_id          UUID NOT NULL,
    user2_id          UUID NOT NULL,
    is_liked_by_user1 BOOLEAN,
    is_liked_by_user2 BOOLEAN,
    updated_at        TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),

    CONSTRAINT pk_swipes PRIMARY KEY (user1_id, user2_id),
    CONSTRAINT chk_user_order CHECK (user1_id < user2_id)
);
-- rollback DROP TABLE swipes;