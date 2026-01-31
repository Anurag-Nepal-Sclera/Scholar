-- V6: Remove professor_keyword table and add email_option table for multiple AI options

-- 1. Create email_option table
CREATE TABLE email_option (
    id UUID PRIMARY KEY,
    email_log_id UUID NOT NULL,
    body TEXT NOT NULL,
    is_selected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_option_log FOREIGN KEY (email_log_id) REFERENCES email_log(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_option_log ON email_option(email_log_id);

-- 2. Add selected_option_id to email_log (optional, but helps keep track)
-- Actually, the current 'body' in email_log can store the selected one.

-- 3. Drop professor_keyword table
DROP TABLE IF EXISTS professor_keyword CASCADE;
