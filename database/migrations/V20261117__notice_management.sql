-- Apply once before deploying notice management to an existing MySQL database.
CREATE TABLE notices (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    ticker BIT NOT NULL,
    visible BIT NOT NULL,
    platform VARCHAR(10) NOT NULL,
    sort_order INT NOT NULL,
    url VARCHAR(2048),
    deleted BIT NOT NULL,
    modified BIT NOT NULL,
    published BIT NOT NULL,
    published_title VARCHAR(200),
    published_content TEXT,
    published_ticker BIT NOT NULL,
    published_platform VARCHAR(10),
    published_sort_order INT NOT NULL,
    published_url VARCHAR(2048)
);

CREATE TABLE notice_edit_lock (id BIGINT NOT NULL PRIMARY KEY);
INSERT INTO notice_edit_lock (id) VALUES (1);
