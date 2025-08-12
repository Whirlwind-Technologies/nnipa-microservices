-- Initial Database Setup for Tenant Management Service
-- This script runs as the postgres superuser during initialization

-- Create the tenant_user if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'tenant_user') THEN
        CREATE USER tenant_user WITH PASSWORD 'tenant_pass';
END IF;
END
$$;

-- Create the tenant_db database if it doesn't exist
SELECT 'CREATE DATABASE tenant_db OWNER tenant_user'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'tenant_db')\gexec

-- Grant all privileges to tenant_user
GRANT ALL PRIVILEGES ON DATABASE tenant_db TO tenant_user;

-- Connect to tenant_db for remaining operations
\connect tenant_db;

-- Create extensions as superuser (before switching to tenant_user)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create tenant_registry schema
CREATE SCHEMA IF NOT EXISTS tenant_registry AUTHORIZATION tenant_user;

-- Grant permissions
GRANT ALL ON SCHEMA tenant_registry TO tenant_user;
GRANT ALL ON SCHEMA public TO tenant_user;
GRANT CREATE ON SCHEMA public TO tenant_user;

-- Grant extension usage
GRANT USAGE ON SCHEMA public TO tenant_user;

-- Set default privileges for tenant_user
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO tenant_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO tenant_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO tenant_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_registry GRANT ALL ON TABLES TO tenant_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_registry GRANT ALL ON SEQUENCES TO tenant_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA tenant_registry GRANT ALL ON FUNCTIONS TO tenant_user;

-- Make tenant_user a superuser temporarily for migrations (optional, more secure to keep limited)
-- ALTER USER tenant_user WITH SUPERUSER;

-- Or give specific permissions needed for migrations
GRANT CREATE ON DATABASE tenant_db TO tenant_user;

-- Set search path
ALTER DATABASE tenant_db SET search_path TO tenant_registry, public;

-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully';
END
$$;