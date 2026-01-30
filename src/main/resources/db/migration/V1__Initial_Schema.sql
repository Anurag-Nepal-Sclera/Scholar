-- Schema Version: V1__Initial_Schema.sql
-- Description: Create initial database schema with multi-tenant support
-- Author: Scholar Team
-- Date: 2026-01-30

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- TENANT MANAGEMENT
-- ============================================================================

CREATE TABLE tenant (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_tenant_email ON tenant(email);
CREATE INDEX idx_tenant_status ON tenant(status);

-- ============================================================================
-- USER PROFILE MANAGEMENT
-- ============================================================================

CREATE TABLE user_profile (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_profile_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_profile_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT uq_user_profile_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_user_profile_tenant ON user_profile(tenant_id);
CREATE INDEX idx_user_profile_email ON user_profile(email);
CREATE INDEX idx_user_profile_status ON user_profile(status);

-- ============================================================================
-- CV MANAGEMENT
-- ============================================================================

CREATE TABLE cv (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    user_profile_id UUID NOT NULL,
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    raw_text TEXT,
    parsing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    parsing_error_message TEXT,
    parsed_at TIMESTAMP,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_cv_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_cv_user_profile FOREIGN KEY (user_profile_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    CONSTRAINT chk_cv_parsing_status CHECK (parsing_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_cv_file_size CHECK (file_size_bytes > 0)
);

CREATE INDEX idx_cv_tenant ON cv(tenant_id);
CREATE INDEX idx_cv_user_profile ON cv(user_profile_id);
CREATE INDEX idx_cv_parsing_status ON cv(parsing_status);
CREATE INDEX idx_cv_uploaded_at ON cv(uploaded_at DESC);

-- ============================================================================
-- CV KEYWORDS
-- ============================================================================

CREATE TABLE cv_keyword (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    cv_id UUID NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    normalized_keyword VARCHAR(255) NOT NULL,
    weight DECIMAL(5,4) NOT NULL DEFAULT 1.0000,
    frequency INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_cv_keyword_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_cv_keyword_cv FOREIGN KEY (cv_id) REFERENCES cv(id) ON DELETE CASCADE,
    CONSTRAINT chk_cv_keyword_weight CHECK (weight >= 0 AND weight <= 1),
    CONSTRAINT chk_cv_keyword_frequency CHECK (frequency > 0),
    CONSTRAINT uq_cv_keyword_cv_normalized UNIQUE (cv_id, normalized_keyword)
);

CREATE INDEX idx_cv_keyword_tenant ON cv_keyword(tenant_id);
CREATE INDEX idx_cv_keyword_cv ON cv_keyword(cv_id);
CREATE INDEX idx_cv_keyword_normalized ON cv_keyword(normalized_keyword);
CREATE INDEX idx_cv_keyword_weight ON cv_keyword(weight DESC);

-- ============================================================================
-- UNIVERSITY MANAGEMENT
-- ============================================================================

CREATE TABLE university (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(500) NOT NULL,
    country VARCHAR(100) NOT NULL,
    website VARCHAR(500),
    rank_global INT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_university_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX idx_university_name ON university(name);
CREATE INDEX idx_university_country ON university(country);
CREATE INDEX idx_university_rank ON university(rank_global);
CREATE INDEX idx_university_status ON university(status);

-- ============================================================================
-- PROFESSOR MANAGEMENT
-- ============================================================================

CREATE TABLE professor (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    university_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    department VARCHAR(255),
    research_area TEXT,
    profile_url VARCHAR(1000),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_professor_university FOREIGN KEY (university_id) REFERENCES university(id) ON DELETE RESTRICT,
    CONSTRAINT chk_professor_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE INDEX idx_professor_university ON professor(university_id);
CREATE INDEX idx_professor_email ON professor(email);
CREATE INDEX idx_professor_name ON professor(last_name, first_name);
CREATE INDEX idx_professor_department ON professor(department);
CREATE INDEX idx_professor_status ON professor(status);

-- ============================================================================
-- PROFESSOR KEYWORDS
-- ============================================================================

CREATE TABLE professor_keyword (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    professor_id UUID NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    normalized_keyword VARCHAR(255) NOT NULL,
    weight DECIMAL(5,4) NOT NULL DEFAULT 1.0000,
    source VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_professor_keyword_professor FOREIGN KEY (professor_id) REFERENCES professor(id) ON DELETE CASCADE,
    CONSTRAINT chk_professor_keyword_weight CHECK (weight >= 0 AND weight <= 1),
    CONSTRAINT chk_professor_keyword_source CHECK (source IN ('RESEARCH_AREA', 'PUBLICATION', 'MANUAL')),
    CONSTRAINT uq_professor_keyword_professor_normalized UNIQUE (professor_id, normalized_keyword)
);

CREATE INDEX idx_professor_keyword_professor ON professor_keyword(professor_id);
CREATE INDEX idx_professor_keyword_normalized ON professor_keyword(normalized_keyword);
CREATE INDEX idx_professor_keyword_weight ON professor_keyword(weight DESC);

-- ============================================================================
-- MATCH RESULTS
-- ============================================================================

CREATE TABLE match_result (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    cv_id UUID NOT NULL,
    professor_id UUID NOT NULL,
    match_score DECIMAL(8,6) NOT NULL,
    matched_keywords TEXT,
    total_cv_keywords INT NOT NULL,
    total_professor_keywords INT NOT NULL,
    total_matched_keywords INT NOT NULL,
    computed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_match_result_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_result_cv FOREIGN KEY (cv_id) REFERENCES cv(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_result_professor FOREIGN KEY (professor_id) REFERENCES professor(id) ON DELETE CASCADE,
    CONSTRAINT chk_match_result_score CHECK (match_score >= 0 AND match_score <= 1),
    CONSTRAINT chk_match_result_counts CHECK (total_cv_keywords >= 0 AND total_professor_keywords >= 0 AND total_matched_keywords >= 0),
    CONSTRAINT uq_match_result_cv_professor UNIQUE (cv_id, professor_id)
);

CREATE INDEX idx_match_result_tenant ON match_result(tenant_id);
CREATE INDEX idx_match_result_cv ON match_result(cv_id);
CREATE INDEX idx_match_result_professor ON match_result(professor_id);
CREATE INDEX idx_match_result_score ON match_result(match_score DESC);
CREATE INDEX idx_match_result_computed_at ON match_result(computed_at DESC);

-- ============================================================================
-- SMTP ACCOUNT MANAGEMENT
-- ============================================================================

CREATE TABLE smtp_account (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    smtp_host VARCHAR(255) NOT NULL,
    smtp_port INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    encrypted_password TEXT NOT NULL,
    use_tls BOOLEAN NOT NULL DEFAULT TRUE,
    use_ssl BOOLEAN NOT NULL DEFAULT FALSE,
    from_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_smtp_account_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT chk_smtp_account_port CHECK (smtp_port > 0 AND smtp_port <= 65535),
    CONSTRAINT chk_smtp_account_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT uq_smtp_account_tenant UNIQUE (tenant_id)
);

CREATE INDEX idx_smtp_account_tenant ON smtp_account(tenant_id);
CREATE INDEX idx_smtp_account_status ON smtp_account(status);

-- ============================================================================
-- EMAIL CAMPAIGNS
-- ============================================================================

CREATE TABLE email_campaign (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    cv_id UUID NOT NULL,
    smtp_account_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_template TEXT NOT NULL,
    min_match_score DECIMAL(5,4) NOT NULL DEFAULT 0.5000,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    total_recipients INT NOT NULL DEFAULT 0,
    sent_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_email_campaign_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_email_campaign_cv FOREIGN KEY (cv_id) REFERENCES cv(id) ON DELETE RESTRICT,
    CONSTRAINT fk_email_campaign_smtp FOREIGN KEY (smtp_account_id) REFERENCES smtp_account(id) ON DELETE RESTRICT,
    CONSTRAINT chk_email_campaign_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_email_campaign_min_score CHECK (min_match_score >= 0 AND min_match_score <= 1),
    CONSTRAINT chk_email_campaign_counts CHECK (sent_count >= 0 AND failed_count >= 0 AND total_recipients >= 0)
);

CREATE INDEX idx_email_campaign_tenant ON email_campaign(tenant_id);
CREATE INDEX idx_email_campaign_cv ON email_campaign(cv_id);
CREATE INDEX idx_email_campaign_status ON email_campaign(status);
CREATE INDEX idx_email_campaign_scheduled ON email_campaign(scheduled_at);

-- ============================================================================
-- EMAIL LOGS
-- ============================================================================

CREATE TABLE email_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    email_campaign_id UUID NOT NULL,
    professor_id UUID NOT NULL,
    match_result_id UUID NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_email_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_email_log_campaign FOREIGN KEY (email_campaign_id) REFERENCES email_campaign(id) ON DELETE CASCADE,
    CONSTRAINT fk_email_log_professor FOREIGN KEY (professor_id) REFERENCES professor(id) ON DELETE RESTRICT,
    CONSTRAINT fk_email_log_match_result FOREIGN KEY (match_result_id) REFERENCES match_result(id) ON DELETE RESTRICT,
    CONSTRAINT chk_email_log_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'BLACKLISTED')),
    CONSTRAINT chk_email_log_retry_count CHECK (retry_count >= 0),
    CONSTRAINT uq_email_log_campaign_professor UNIQUE (email_campaign_id, professor_id)
);

CREATE INDEX idx_email_log_tenant ON email_log(tenant_id);
CREATE INDEX idx_email_log_campaign ON email_log(email_campaign_id);
CREATE INDEX idx_email_log_professor ON email_log(professor_id);
CREATE INDEX idx_email_log_status ON email_log(status);
CREATE INDEX idx_email_log_sent_at ON email_log(sent_at DESC);

-- ============================================================================
-- EMAIL BLACKLIST
-- ============================================================================

CREATE TABLE email_blacklist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID,
    email VARCHAR(255) NOT NULL,
    reason VARCHAR(500),
    blacklisted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_email_blacklist_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_blacklist_tenant ON email_blacklist(tenant_id);
CREATE INDEX idx_email_blacklist_email ON email_blacklist(email);
CREATE UNIQUE INDEX uq_email_blacklist_tenant_email ON email_blacklist(COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid), email);

-- ============================================================================
-- TRIGGERS FOR UPDATED_AT
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tenant_updated_at BEFORE UPDATE ON tenant
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_profile_updated_at BEFORE UPDATE ON user_profile
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_university_updated_at BEFORE UPDATE ON university
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_professor_updated_at BEFORE UPDATE ON professor
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_smtp_account_updated_at BEFORE UPDATE ON smtp_account
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_email_campaign_updated_at BEFORE UPDATE ON email_campaign
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
