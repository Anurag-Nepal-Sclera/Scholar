-- Migration Version: V3__Add_Auth_Fields.sql
-- Description: Add password and role columns to user_profile
-- Author: Scholar Team
-- Date: 2026-01-30

ALTER TABLE user_profile ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT 'temporary_password';
ALTER TABLE user_profile ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'USER';

-- Remove default after update if needed, but for existing it's better to keep it or update manually
ALTER TABLE user_profile ALTER COLUMN password DROP DEFAULT;
