-- Apply once before deploying terms management to an existing MySQL database.
CREATE TABLE terms_revisions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    content MEDIUMTEXT NOT NULL,
    published_at DATETIME(6) NOT NULL
);

CREATE TABLE terms_edit_lock (id BIGINT NOT NULL PRIMARY KEY);
INSERT INTO terms_edit_lock (id) VALUES (1);
