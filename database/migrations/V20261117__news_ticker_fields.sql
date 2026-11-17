-- Apply after V20261117__notice_management.sql and before deploying news tickers.
ALTER TABLE notices
    ADD COLUMN kind VARCHAR(10) NOT NULL DEFAULT 'NOTICE',
    ADD COLUMN title_color VARCHAR(7),
    ADD COLUMN content_color VARCHAR(7),
    ADD COLUMN published_title_color VARCHAR(7),
    ADD COLUMN published_content_color VARCHAR(7);
