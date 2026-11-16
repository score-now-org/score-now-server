-- Apply once before deploying forbidden word management to existing MySQL databases.
CREATE TABLE forbidden_words (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL,
    replacement VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_forbidden_word UNIQUE (word)
);
