-- Historical reference: Flyway now checks and applies this change automatically.
-- Do not run this script manually after the automatic migration.
ALTER TABLE chat_messages
    DROP FOREIGN KEY fk_chat_messages_user;

ALTER TABLE chat_messages
    MODIFY COLUMN user_id BIGINT NULL;

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_messages_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL;
