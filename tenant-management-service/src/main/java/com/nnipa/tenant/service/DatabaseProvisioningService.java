package com.nnipa.tenant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service for provisioning tenant database schemas.
 * Handles creation and initialization of tenant-specific database structures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseProvisioningService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Create a new schema for the tenant
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTenantSchema(String schemaName) {
        log.info("Creating database schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Create schema
            String createSchemaSQL = String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName);
            statement.execute(createSchemaSQL);

            // Grant permissions to tenant_user
            String grantSQL = String.format("GRANT ALL ON SCHEMA %s TO tenant_user", schemaName);
            statement.execute(grantSQL);

            log.info("Successfully created schema: {}", schemaName);

        } catch (SQLException e) {
            log.error("Failed to create schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create tenant schema", e);
        }
    }

    /**
     * Initialize tables in the tenant schema
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initializeTenantTables(String schemaName) {
        log.info("Initializing tables for schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Set search path to tenant schema
            statement.execute(String.format("SET search_path TO %s", schemaName));

            // Create datasets table
            String createDatasetsTable = """
                CREATE TABLE IF NOT EXISTS datasets (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    type VARCHAR(50),
                    size_bytes BIGINT DEFAULT 0,
                    record_count BIGINT DEFAULT 0,
                    metadata JSONB,
                    tags JSONB,
                    is_public BOOLEAN DEFAULT false,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by VARCHAR(100),
                    updated_by VARCHAR(100)
                )
                """;
            statement.execute(createDatasetsTable);

            // Create surveys table
            String createSurveysTable = """
                CREATE TABLE IF NOT EXISTS surveys (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    type VARCHAR(50),
                    status VARCHAR(20) DEFAULT 'DRAFT',
                    questions JSONB,
                    settings JSONB,
                    response_count INTEGER DEFAULT 0,
                    start_date TIMESTAMP WITH TIME ZONE,
                    end_date TIMESTAMP WITH TIME ZONE,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by VARCHAR(100),
                    updated_by VARCHAR(100)
                )
                """;
            statement.execute(createSurveysTable);

            // Create models table
            String createModelsTable = """
                CREATE TABLE IF NOT EXISTS models (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    type VARCHAR(50),
                    algorithm VARCHAR(100),
                    parameters JSONB,
                    metrics JSONB,
                    status VARCHAR(20) DEFAULT 'DRAFT',
                    version VARCHAR(20),
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by VARCHAR(100),
                    updated_by VARCHAR(100)
                )
                """;
            statement.execute(createModelsTable);

            // Create dashboards table
            String createDashboardsTable = """
                CREATE TABLE IF NOT EXISTS dashboards (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    type VARCHAR(50),
                    layout JSONB,
                    widgets JSONB,
                    settings JSONB,
                    is_public BOOLEAN DEFAULT false,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    created_by VARCHAR(100),
                    updated_by VARCHAR(100)
                )
                """;
            statement.execute(createDashboardsTable);

            // Create users table (tenant-specific users)
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    username VARCHAR(100) UNIQUE NOT NULL,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    first_name VARCHAR(100),
                    last_name VARCHAR(100),
                    role VARCHAR(50) DEFAULT 'USER',
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    last_login TIMESTAMP WITH TIME ZONE,
                    metadata JSONB,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                )
                """;
            statement.execute(createUsersTable);

            // Create audit_logs table
            String createAuditLogsTable = """
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    user_id UUID,
                    action VARCHAR(100) NOT NULL,
                    entity_type VARCHAR(50),
                    entity_id UUID,
                    old_value JSONB,
                    new_value JSONB,
                    ip_address VARCHAR(45),
                    user_agent TEXT,
                    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                )
                """;
            statement.execute(createAuditLogsTable);

            // Create indexes
            createIndexes(statement, schemaName);

            log.info("Successfully initialized tables for schema: {}", schemaName);

        } catch (SQLException e) {
            log.error("Failed to initialize tables for schema: {}", schemaName, e);
            throw new RuntimeException("Failed to initialize tenant tables", e);
        }
    }

    /**
     * Create indexes for tenant tables
     */
    private void createIndexes(Statement statement, String schemaName) throws SQLException {
        log.debug("Creating indexes for schema: {}", schemaName);

        // Datasets indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_datasets_name ON datasets(name)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_datasets_created ON datasets(created_at)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_datasets_public ON datasets(is_public)");

        // Surveys indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_surveys_name ON surveys(name)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_surveys_status ON surveys(status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_surveys_dates ON surveys(start_date, end_date)");

        // Models indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_models_name ON models(name)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_models_status ON models(status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_models_type ON models(type)");

        // Dashboards indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_dashboards_name ON dashboards(name)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_dashboards_public ON dashboards(is_public)");

        // Users indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_users_status ON users(status)");

        // Audit logs indexes
        statement.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp)");
    }

    /**
     * Insert default data for the tenant
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertDefaultData(String schemaName, String tenantName) {
        log.info("Inserting default data for schema: {}", schemaName);

        try {
            // Set search path
            jdbcTemplate.execute(String.format("SET search_path TO %s", schemaName));

            // Insert default admin user
            String insertAdminUser = """
                INSERT INTO users (username, email, first_name, last_name, role, status)
                VALUES (?, ?, 'Admin', 'User', 'ADMIN', 'ACTIVE')
                ON CONFLICT (username) DO NOTHING
                """;
            jdbcTemplate.update(insertAdminUser,
                    "admin_" + schemaName,
                    "admin@" + tenantName.toLowerCase().replace(" ", "") + ".com");

            // Insert sample dashboard
            String insertDashboard = """
                INSERT INTO dashboards (name, description, type, layout, widgets, is_public)
                VALUES (?, ?, 'DEFAULT', '{}', '[]', false)
                """;
            jdbcTemplate.update(insertDashboard,
                    "Welcome Dashboard",
                    "Default dashboard for " + tenantName);

            // Insert sample dataset
            String insertDataset = """
                INSERT INTO datasets (name, description, type, metadata)
                VALUES (?, ?, 'SAMPLE', '{"source": "system", "format": "csv"}')
                """;
            jdbcTemplate.update(insertDataset,
                    "Sample Dataset",
                    "Sample dataset to get started");

            // Log initial audit entry
            String insertAuditLog = """
                INSERT INTO audit_logs (action, entity_type, new_value)
                VALUES ('TENANT_PROVISIONED', 'TENANT', ?)
                """;
            jdbcTemplate.update(insertAuditLog,
                    String.format("{\"schema\": \"%s\", \"name\": \"%s\"}", schemaName, tenantName));

            log.info("Successfully inserted default data for schema: {}", schemaName);

        } catch (Exception e) {
            log.error("Failed to insert default data for schema: {}", schemaName, e);
            throw new RuntimeException("Failed to insert default data", e);
        }
    }

    /**
     * Drop tenant schema (used for cleanup or deletion)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dropTenantSchema(String schemaName) {
        log.warn("Dropping database schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Drop schema cascade (removes all objects in the schema)
            String dropSchemaSQL = String.format("DROP SCHEMA IF EXISTS %s CASCADE", schemaName);
            statement.execute(dropSchemaSQL);

            log.info("Successfully dropped schema: {}", schemaName);

        } catch (SQLException e) {
            log.error("Failed to drop schema: {}", schemaName, e);
            throw new RuntimeException("Failed to drop tenant schema", e);
        }
    }

    /**
     * Check if schema exists
     */
    public boolean schemaExists(String schemaName) {
        String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schemaName);
        return exists != null && exists;
    }

    /**
     * Get schema size in bytes
     */
    public Long getSchemaSize(String schemaName) {
        String sql = """
            SELECT SUM(pg_total_relation_size(quote_ident(schemaname)||'.'||quote_ident(tablename)))::BIGINT 
            FROM pg_tables 
            WHERE schemaname = ?
            """;
        return jdbcTemplate.queryForObject(sql, Long.class, schemaName);
    }

    /**
     * Get table count in schema
     */
    public Integer getTableCount(String schemaName) {
        String sql = """
            SELECT COUNT(*) 
            FROM information_schema.tables 
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            """;
        return jdbcTemplate.queryForObject(sql, Integer.class, schemaName);
    }
}