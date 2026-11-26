-- Apply once before deploying help management to an existing MySQL database.
CREATE TABLE help_documents (
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    draft_json MEDIUMTEXT NOT NULL,
    published_json MEDIUMTEXT NOT NULL
);
