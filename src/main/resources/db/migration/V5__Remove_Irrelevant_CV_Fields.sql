-- Remove redundant/irrelevant fields from CV table
ALTER TABLE cv DROP COLUMN IF EXISTS raw_text;
ALTER TABLE cv DROP COLUMN IF EXISTS parsing_error_message;
