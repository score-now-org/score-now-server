-- Apply once if Hibernate ddl-auto has not already created the column.
ALTER TABLE inquiries ADD COLUMN answer_html TEXT NULL;
