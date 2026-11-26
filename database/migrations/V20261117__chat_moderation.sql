-- Apply once to existing MySQL databases before deploying chat moderation.
ALTER TABLE chat_messages
    ADD COLUMN image_url VARCHAR(1000) NULL,
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deletion_reason VARCHAR(500) NULL,
    ADD COLUMN image_removed_at DATETIME(6) NULL,
    ADD COLUMN image_deletion_reason VARCHAR(500) NULL,
    ADD COLUMN report_count BIGINT NULL;

CREATE INDEX idx_chat_messages_report_count_id ON chat_messages (report_count, id);

CREATE TABLE chat_message_reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    reporter_id BIGINT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_chat_report_user UNIQUE (message_id, reporter_id),
    INDEX idx_chat_report_message (message_id),
    CONSTRAINT fk_chat_report_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_report_user FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE SET NULL
);
