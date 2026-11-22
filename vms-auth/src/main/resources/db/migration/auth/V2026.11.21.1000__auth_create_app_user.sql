-- Module: auth  | Owner: @datavvy/platform-team
-- Versions are timestamps (VYYYY.MM.DD.HHMM) so parallel teams never collide.

CREATE TABLE app_user (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    email            VARCHAR(190) NOT NULL,
    display_name     VARCHAR(120) NOT NULL,
    password_hash    VARCHAR(100) NOT NULL,
    role             VARCHAR(30)  NOT NULL,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    created_by       VARCHAR(120) NULL,
    updated_by       VARCHAR(120) NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);
