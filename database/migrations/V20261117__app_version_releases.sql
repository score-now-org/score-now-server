-- Apply once to the target MySQL database before deploying version history.
CREATE TABLE app_version_releases (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version_name VARCHAR(40) NOT NULL,
    platform VARCHAR(16) NOT NULL,
    release_date DATE NOT NULL,
    content VARCHAR(3000) NOT NULL,
    INDEX idx_app_version_release_date (release_date, id)
);
