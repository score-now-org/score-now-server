-- Apply once to existing MySQL databases before deploying member moderation.
-- Existing status and token data are preserved; a null token_version means version 0.
ALTER TABLE users
    MODIFY COLUMN status ENUM('ACTIVE','INACTIVE','BANNED','DELETED','PERMANENTLY_WITHDRAWN') NULL,
    ADD COLUMN warning_reason VARCHAR(500) NULL,
    ADD COLUMN warned_at DATETIME(6) NULL,
    ADD COLUMN permanent_withdrawal_reason VARCHAR(500) NULL,
    ADD COLUMN permanently_withdrawn_at DATETIME(6) NULL,
    ADD COLUMN status_before_permanent_withdrawal ENUM('ACTIVE','INACTIVE','BANNED','DELETED','PERMANENTLY_WITHDRAWN') NULL,
    ADD COLUMN token_version BIGINT NULL;

CREATE TABLE member_moderation_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_member_moderation_user (user_id, id),
    CONSTRAINT fk_member_moderation_user FOREIGN KEY (user_id) REFERENCES users(id)
);
