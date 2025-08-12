-- V2__Insert_sample_data.sql
-- Insert sample data for development and testing

SET search_path TO tenant_registry;

-- Insert sample tenants
INSERT INTO tenants (
    id, name, display_name, subdomain, schema_name, status, subscription_plan,
    description, contact_email, timezone, locale, currency,
    max_users, max_storage_gb, max_datasets,
    created_by, updated_by
) VALUES
      (
          'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
          'Acme Corporation',
          'Acme Corp',
          'acme',
          'tenant_acme',
          'ACTIVE',
          'PROFESSIONAL',
          'Leading technology company specializing in data analytics',
          'admin@acme.com',
          'America/New_York',
          'en_US',
          'USD',
          200,
          500,
          2000,
          'system',
          'system'
      ),
      (
          'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
          'Demo Organization',
          'Demo Org',
          'demo',
          'tenant_demo',
          'ACTIVE',
          'FREE',
          'Demo organization for testing purposes',
          'demo@example.com',
          'UTC',
          'en_US',
          'USD',
          10,
          25,
          100,
          'system',
          'system'
      );

-- Insert sample features for Acme Corporation
INSERT INTO tenant_features (tenant_id, feature, enabled, created_by) VALUES
                                                                          ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'BASIC_ANALYTICS', true, 'system'),
                                                                          ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ADVANCED_ANALYTICS', true, 'system'),
                                                                          ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DATA_EXPORT', true, 'system'),
                                                                          ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'API_ACCESS', true, 'system'),
                                                                          ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'CUSTOM_BRANDING', true, 'system'),
                                                                          ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PRIORITY_SUPPORT', true, 'system');

-- Insert sample features for Demo Organization
INSERT INTO tenant_features (tenant_id, feature, enabled, created_by) VALUES
                                                                          ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'BASIC_ANALYTICS', true, 'system'),
                                                                          ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'DATA_EXPORT', true, 'system'),
                                                                          ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'API_ACCESS', true, 'system');

-- Insert sample configurations
INSERT INTO tenant_configurations (
    tenant_id, config_key, config_value, category, data_type, created_by
) VALUES
      ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'max_api_requests_per_minute', '1000', 'API', 'NUMBER', 'system'),
      ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'enable_2fa', 'true', 'SECURITY', 'BOOLEAN', 'system'),
      ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'data_retention_days', '365', 'DATA', 'NUMBER', 'system'),
      ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'max_api_requests_per_minute', '100', 'API', 'NUMBER', 'system'),
      ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'enable_2fa', 'false', 'SECURITY', 'BOOLEAN', 'system'),
      ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'data_retention_days', '30', 'DATA', 'NUMBER', 'system');

-- Insert sample subscription for Acme Corporation
INSERT INTO tenant_subscriptions (
    tenant_id, plan, status, start_date, billing_cycle,
    price, currency, auto_renew, created_by
) VALUES
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'PROFESSIONAL',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        'MONTHLY',
        199.99,
        'USD',
        true,
        'system'
    );

-- Insert sample subscription for Demo Organization
INSERT INTO tenant_subscriptions (
    tenant_id, plan, status, start_date, trial_start_date, trial_end_date,
    billing_cycle, price, currency, auto_renew, created_by
) VALUES
    (
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12',
        'FREE',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP + INTERVAL '30 days',
        'MONTHLY',
        0.00,
        'USD',
        false,
        'system'
    );

-- Insert sample usage data for current month
INSERT INTO tenant_usage (
    tenant_id, usage_date, metric_name, metric_value, metric_unit,
    metric_category, is_billable, created_by
) VALUES
      ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', CURRENT_DATE, 'storage_gb', 125.5, 'GB', 'STORAGE', true, 'system'),
      ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', CURRENT_DATE, 'api_calls', 50000, 'COUNT', 'API', true, 'system'),
      ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', CURRENT_DATE, 'active_users', 45, 'USERS', 'USERS', false, 'system'),
      ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', CURRENT_DATE, 'storage_gb', 2.5, 'GB', 'STORAGE', false, 'system'),
      ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', CURRENT_DATE, 'api_calls', 1000, 'COUNT', 'API', false, 'system'),
      ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', CURRENT_DATE, 'active_users', 3, 'USERS', 'USERS', false, 'system');