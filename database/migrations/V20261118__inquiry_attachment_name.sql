-- Apply once if Hibernate ddl-auto has not already created the column.
-- Existing image inquiries remain accessible; a null name identifies legacy images.
ALTER TABLE inquiries ADD COLUMN attachment_name VARCHAR(180) NULL;
