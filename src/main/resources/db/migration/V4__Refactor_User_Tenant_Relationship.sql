-- Migration Version: V4__Refactor_User_Tenant_Relationship.sql
-- Description: Refactor relationship so User owns multiple Tenants
-- Author: Scholar Team
-- Date: 2026-01-30

-- 1. Remove foreign key and column from user_profile
ALTER TABLE user_profile DROP CONSTRAINT IF EXISTS fk_user_profile_tenant;
ALTER TABLE user_profile DROP COLUMN IF EXISTS tenant_id;

-- 2. Add owner_id to tenant
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS owner_id UUID;

-- 3. Add foreign key from tenant to user_profile
ALTER TABLE tenant ADD CONSTRAINT fk_tenant_owner FOREIGN KEY (owner_id) REFERENCES user_profile(id) ON DELETE CASCADE;

-- 4. Create index on owner_id
CREATE INDEX IF NOT EXISTS idx_tenant_owner ON tenant(owner_id);

-- 5. Ensure user_profile email is unique
-- First drop existing if any (Flyway might have handled partials)
DROP INDEX IF EXISTS idx_user_profile_email;
CREATE UNIQUE INDEX uq_user_profile_email ON user_profile(email);

-- 6. Modify tenant email to NOT be unique
ALTER TABLE tenant DROP CONSTRAINT IF EXISTS tenant_email_key;
