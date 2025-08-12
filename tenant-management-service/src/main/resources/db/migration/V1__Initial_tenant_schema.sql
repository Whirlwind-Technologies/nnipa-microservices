-- V1__Initial_tenant_schema.sql
-- Initial schema creation for Tenant Management Service

-- Set search path
SET search_path TO tenant_registry, public;

-- Ensure extensions are created (in case they weren't created during init)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "pgcrypto" SCHEMA public;

-- Ensure schema exists
CREATE SCHEMA IF NOT EXISTS tenant_registry;

-- Create tenants table
CREATE TABLE IF NOT EXISTS tenant_registry.tenants (
                                                       id UUID PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    subdomain VARCHAR(100) NOT NULL UNIQUE,
    schema_name VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subscription_plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    description TEXT,
    logo_url VARCHAR(500),
    website VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),

    -- Address fields (embedded)
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2),

    -- Localization
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en_US',
    currency VARCHAR(3) DEFAULT 'USD',

    -- Subscription dates
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    subscription_ends_at TIMESTAMP WITH TIME ZONE,
    suspended_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Resource limits
    max_users INTEGER DEFAULT 10,
    max_storage_gb INTEGER DEFAULT 25,
    max_datasets INTEGER DEFAULT 100,

    -- Current usage (cached)
    current_users INTEGER DEFAULT 0,
    current_storage_gb DOUBLE PRECISION DEFAULT 0,
    current_datasets INTEGER DEFAULT 0,

    -- Metadata
    metadata JSONB,
    tags JSONB,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROVISIONING', 'ACTIVE', 'SUSPENDED', 'INACTIVE', 'DELETED', 'ARCHIVED')),
    CONSTRAINT chk_subscription_plan CHECK (subscription_plan IN ('FREE', 'STARTER', 'PROFESSIONAL', 'ENTERPRISE', 'EDUCATION'))
    );

-- Create indexes for tenants table
CREATE INDEX IF NOT EXISTS idx_tenant_subdomain ON tenant_registry.tenants(subdomain);
CREATE INDEX IF NOT EXISTS idx_tenant_status ON tenant_registry.tenants(status);
CREATE INDEX IF NOT EXISTS idx_tenant_plan ON tenant_registry.tenants(subscription_plan);
CREATE INDEX IF NOT EXISTS idx_tenant_created ON tenant_registry.tenants(created_at);
CREATE INDEX IF NOT EXISTS idx_tenant_deleted ON tenant_registry.tenants(deleted_at) WHERE deleted_at IS NOT NULL;

-- Create tenant_features table
CREATE TABLE IF NOT EXISTS tenant_registry.tenant_features (
                                                               id UUID PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenant_registry.tenants(id) ON DELETE CASCADE,
    feature VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    expires_at TIMESTAMP WITH TIME ZONE,
    configuration JSONB,
    notes TEXT,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                                       created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,

    CONSTRAINT uk_tenant_feature UNIQUE (tenant_id, feature)
    );

-- Create indexes for tenant_features table
CREATE INDEX IF NOT EXISTS idx_tenant_features_tenant ON tenant_registry.tenant_features(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_features_enabled ON tenant_registry.tenant_features(enabled);
CREATE INDEX IF NOT EXISTS idx_tenant_features_expires ON tenant_registry.tenant_features(expires_at) WHERE expires_at IS NOT NULL;

-- Create tenant_configurations table
CREATE TABLE IF NOT EXISTS tenant_registry.tenant_configurations (
                                                                     id UUID PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenant_registry.tenants(id) ON DELETE CASCADE,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    category VARCHAR(50),
    data_type VARCHAR(20),
    is_encrypted BOOLEAN NOT NULL DEFAULT false,
    is_sensitive BOOLEAN NOT NULL DEFAULT false,
    description TEXT,
    default_value TEXT,
    validation_rules JSONB,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                                       created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,

    CONSTRAINT uk_tenant_config_key UNIQUE (tenant_id, config_key)
    );

-- Create indexes for tenant_configurations table
CREATE INDEX IF NOT EXISTS idx_tenant_configs_tenant ON tenant_registry.tenant_configurations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_configs_category ON tenant_registry.tenant_configurations(category);

-- Create tenant_subscriptions table
CREATE TABLE IF NOT EXISTS tenant_registry.tenant_subscriptions (
                                                                    id UUID PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenant_registry.tenants(id) ON DELETE CASCADE,
    plan VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE,
    trial_start_date TIMESTAMP WITH TIME ZONE,
    trial_end_date TIMESTAMP WITH TIME ZONE,
                                                                       billing_cycle VARCHAR(20),
    price DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'USD',
    discount_percentage INTEGER,
    discount_amount DECIMAL(10,2),
    final_price DECIMAL(10,2),
    payment_method VARCHAR(50),
    payment_reference VARCHAR(255),
    auto_renew BOOLEAN NOT NULL DEFAULT true,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    notes TEXT,
    metadata JSONB,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                                       created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,

    CONSTRAINT chk_sub_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED', 'PENDING'))
    );

-- Create indexes for tenant_subscriptions table
CREATE INDEX IF NOT EXISTS idx_tenant_subs_tenant ON tenant_registry.tenant_subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_subs_status ON tenant_registry.tenant_subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_tenant_subs_dates ON tenant_registry.tenant_subscriptions(start_date, end_date);

-- Create tenant_usage table
CREATE TABLE IF NOT EXISTS tenant_registry.tenant_usage (
                                                            id UUID PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenant_registry.tenants(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    metric_name VARCHAR(50) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(20),
    metric_category VARCHAR(50),
    peak_value DOUBLE PRECISION,
    average_value DOUBLE PRECISION,
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    sample_count INTEGER,
    cost DECIMAL(10,4),
    currency VARCHAR(3) DEFAULT 'USD',
    is_billable BOOLEAN NOT NULL DEFAULT true,
    billing_period VARCHAR(20),
    metadata JSONB,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                                       created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,

    CONSTRAINT uk_tenant_usage_date UNIQUE (tenant_id, usage_date, metric_name)
    );

-- Create indexes for tenant_usage table
CREATE INDEX IF NOT EXISTS idx_tenant_usage_tenant ON tenant_registry.tenant_usage(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_usage_date ON tenant_registry.tenant_usage(usage_date);
CREATE INDEX IF NOT EXISTS idx_tenant_usage_metric ON tenant_registry.tenant_usage(metric_name);
CREATE INDEX IF NOT EXISTS idx_tenant_usage_category ON tenant_registry.tenant_usage(metric_category);

-- Create update trigger function with proper schema qualification
CREATE OR REPLACE FUNCTION tenant_registry.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for all tables
DROP TRIGGER IF EXISTS update_tenants_updated_at ON tenant_registry.tenants;
CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenant_registry.tenants
    FOR EACH ROW EXECUTE FUNCTION tenant_registry.update_updated_at_column();

DROP TRIGGER IF EXISTS update_tenant_features_updated_at ON tenant_registry.tenant_features;
CREATE TRIGGER update_tenant_features_updated_at BEFORE UPDATE ON tenant_registry.tenant_features
    FOR EACH ROW EXECUTE FUNCTION tenant_registry.update_updated_at_column();

DROP TRIGGER IF EXISTS update_tenant_configurations_updated_at ON tenant_registry.tenant_configurations;
CREATE TRIGGER update_tenant_configurations_updated_at BEFORE UPDATE ON tenant_registry.tenant_configurations
    FOR EACH ROW EXECUTE FUNCTION tenant_registry.update_updated_at_column();

DROP TRIGGER IF EXISTS update_tenant_subscriptions_updated_at ON tenant_registry.tenant_subscriptions;
CREATE TRIGGER update_tenant_subscriptions_updated_at BEFORE UPDATE ON tenant_registry.tenant_subscriptions
    FOR EACH ROW EXECUTE FUNCTION tenant_registry.update_updated_at_column();

DROP TRIGGER IF EXISTS update_tenant_usage_updated_at ON tenant_registry.tenant_usage;
CREATE TRIGGER update_tenant_usage_updated_at BEFORE UPDATE ON tenant_registry.tenant_usage
    FOR EACH ROW EXECUTE FUNCTION tenant_registry.update_updated_at_column();