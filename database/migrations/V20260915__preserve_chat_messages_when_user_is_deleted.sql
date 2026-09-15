-- Apply once to existing MySQL databases before deploying the nullable ChatMessage.user mapping.
-- New databases receive the same constraint from ChatMessage's JPA mapping.
ALTER TABLE chat_messages
    DROP FOREIGN KEY fk_chat_messages_user;

ALTER TABLE chat_messages
    MODIFY COLUMN user_id BIGINT NULL;

ALTER TABLE chat_messages
    ADD CONSTRAINT fk_chat_messages_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL;
