package com.nnipa.tenant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Validate schema name
        validateSchemaName(schemaName);

        try (Connection connection = dataSource.getConnection()) {
            // Ensure autocommit is off for transaction consistency
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                // Check if schema already exists first
                boolean exists = checkSchemaExistsInConnection(connection, schemaName);
                if (exists) {
                    log.warn("Schema {} already exists, skipping creation", schemaName);
                    return;
                }

                // Create schema - use quoted identifier to handle special characters
                String createSchemaSQL = String.format("CREATE SCHEMA \"%s\"", schemaName);
                log.debug("Executing: {}", createSchemaSQL);
                statement.execute(createSchemaSQL);

                // Grant permissions to tenant_user (if user exists)
                try {
                    String grantSQL = String.format("GRANT ALL ON SCHEMA \"%s\" TO tenant_user", schemaName);
                    log.debug("Executing: {}", grantSQL);
                    statement.execute(grantSQL);
                } catch (SQLException e) {
                    log.warn("Failed to grant permissions to tenant_user (user might not exist): {}", e.getMessage());
                    // Don't fail the entire operation for this
                }

                // Commit the transaction
                connection.commit();
                log.info("Successfully created schema: {}", schemaName);

                // Verify schema was created
                boolean verified = checkSchemaExistsInConnection(connection, schemaName);
                if (!verified) {
                    throw new SQLException("Schema creation verification failed for: " + schemaName);
                }

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            log.error("Failed to create schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create tenant schema: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize tables and insert default data in a single transaction.
     * This ensures table creation and data insertion happen atomically.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initializeSchemaWithData(String schemaName, String tenantId) {
        log.info("Initializing tables and data for schema: {}", schemaName);

        validateSchemaName(schemaName);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {

                // First, verify the schema exists
                if (!checkSchemaExistsInConnection(connection, schemaName)) {
                    throw new SQLException("Schema does not exist: " + schemaName);
                }

                // Set search path to tenant schema with quoted identifier
                String setSearchPath = String.format("SET search_path TO \"%s\", public", schemaName);
                log.debug("Setting search path: {}", setSearchPath);
                statement.execute(setSearchPath);

                // Enable UUID extension in the schema
                statement.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");

                // Create all tables
                createAllTablesInSingleTransaction(statement);

                // Create indexes
                createAllIndexesInSingleTransaction(statement);

                // Insert default data
                insertAllDefaultDataInSingleTransaction(statement, schemaName, tenantId);

                // Commit the transaction
                connection.commit();
                log.info("Successfully initialized schema with tables and data: {}", schemaName);

                // Final verification
                int tableCount = getTableCountInConnection(connection, schemaName);
                log.info("Schema {} initialized with {} tables", schemaName, tableCount);

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            log.error("Failed to initialize schema with data: {}", schemaName, e);
            throw new RuntimeException("Failed to initialize tenant schema with data: " + e.getMessage(), e);
        }
    }

    /**
     * Check if schema exists using a specific connection
     */
    private boolean checkSchemaExistsInConnection(Connection connection, String schemaName) throws SQLException {
        String query = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, schemaName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean exists = rs.getBoolean(1);
                    log.debug("Schema {} exists: {}", schemaName, exists);
                    return exists;
                }
            }
        }
        return false;
    }

    /**
     * Get table count using a specific connection
     */
    private int getTableCountInConnection(Connection connection, String schemaName) throws SQLException {
        String query = """
            SELECT COUNT(*) 
            FROM information_schema.tables 
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, schemaName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Validate schema name to prevent SQL injection and ensure valid PostgreSQL identifier
     */
    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }

        // PostgreSQL identifier rules: start with letter or underscore, contain only letters, digits, underscores
        if (!schemaName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName +
                    ". Must start with letter or underscore and contain only letters, digits, and underscores.");
        }

        if (schemaName.length() > 63) {
            throw new IllegalArgumentException("Schema name too long: " + schemaName.length() + " characters (max 63)");
        }
    }

    private void createAllTablesInSingleTransaction(Statement statement) throws SQLException {
        log.debug("Creating all tables in single transaction");

        // Create datasets table
        statement.execute("""
        CREATE TABLE IF NOT EXISTS datasets (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            name VARCHAR(255) NOT NULL,
            description TEXT,
            type VARCHAR(50),
            size_bytes BIGINT DEFAULT 0,
            record_count BIGINT DEFAULT 0,
            schema_definition JSONB,
            metadata JSONB,
            status VARCHAR(20) DEFAULT 'ACTIVE',
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            created_by VARCHAR(100),
            updated_by VARCHAR(100)
        )
        """);

        // Create surveys table
        statement.execute("""
        CREATE TABLE IF NOT EXISTS surveys (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            name VARCHAR(255) NOT NULL,
            description TEXT,
            type VARCHAR(50),
            questions JSONB,
            settings JSONB,
            status VARCHAR(20) DEFAULT 'DRAFT',
            start_date TIMESTAMP WITH TIME ZONE,
            end_date TIMESTAMP WITH TIME ZONE,
            response_count INTEGER DEFAULT 0,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            created_by VARCHAR(100),
            updated_by VARCHAR(100)
        )
        """);

        // Create models table
        statement.execute("""
        CREATE TABLE IF NOT EXISTS models (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            name VARCHAR(255) NOT NULL,
            description TEXT,
            type VARCHAR(50),
            algorithm VARCHAR(100),
            parameters JSONB,
            metrics JSONB,
            version VARCHAR(20),
            status VARCHAR(20) DEFAULT 'DRAFT',
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            created_by VARCHAR(100),
            updated_by VARCHAR(100)
        )
        """);

        // Create dashboards table
        statement.execute("""
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
        """);

        // Create audit_logs table
        statement.execute("""
        CREATE TABLE IF NOT EXISTS audit_logs (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            user_id VARCHAR(100),
            action VARCHAR(100) NOT NULL,
            entity_type VARCHAR(50),
            entity_id UUID,
            old_value JSONB,
            new_value JSONB,
            ip_address VARCHAR(45),
            user_agent TEXT,
            metadata JSONB,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
        )
        """);

        // Create tenant_settings table
        statement.execute("""
        CREATE TABLE IF NOT EXISTS tenant_settings (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            key VARCHAR(100) UNIQUE NOT NULL,
            value TEXT,
            type VARCHAR(20),
            category VARCHAR(50),
            description TEXT,
            is_encrypted BOOLEAN DEFAULT false,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
        )
        """);

        log.debug("All tables created successfully");
    }

    private void createAllIndexesInSingleTransaction(Statement statement) throws SQLException {
        log.debug("Creating all indexes in single transaction");

        statement.execute("CREATE INDEX IF NOT EXISTS idx_datasets_status ON datasets(status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_surveys_status ON surveys(status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_models_status ON models(status)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_dashboards_public ON dashboards(is_public)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id)");

        log.debug("All indexes created successfully");
    }

    private void insertAllDefaultDataInSingleTransaction(Statement statement, String schemaName, String tenantId) throws SQLException {
        log.debug("Inserting all default data in single transaction");

        // Verify dashboards table exists before inserting (safety check)
        ResultSet rs = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'dashboards'"
        );
        rs.next();
        int dashboardTableCount = rs.getInt(1);
        rs.close();

        if (dashboardTableCount == 0) {
            throw new SQLException("Dashboards table was not created successfully in schema: " + schemaName);
        }

        // Insert default dashboard
        statement.execute("""
        INSERT INTO dashboards (name, description, type, layout, is_public)
        VALUES ('Welcome Dashboard', 
                'Default dashboard for getting started', 
                'OVERVIEW',
                '{"type": "grid", "columns": 2, "rows": 2}'::jsonb,
                false)
        """);

        // Insert initial audit log entry
        String insertAuditLog = String.format("""
        INSERT INTO audit_logs (action, entity_type, entity_id, metadata)
        VALUES ('TENANT_PROVISIONED', 
                'TENANT', 
                '%s'::uuid,
                '{"schema": "%s", "provisioned_at": "%s"}'::jsonb)
        """, tenantId, schemaName, java.time.Instant.now());
        statement.execute(insertAuditLog);

        // Insert default tenant settings
        statement.execute("""
        INSERT INTO tenant_settings (key, value, type, category, description)
        VALUES 
            ('date_format', 'YYYY-MM-DD', 'STRING', 'DISPLAY', 'Default date format'),
            ('time_zone', 'UTC', 'STRING', 'DISPLAY', 'Default timezone'),
            ('language', 'en', 'STRING', 'DISPLAY', 'Default language'),
            ('enable_notifications', 'true', 'BOOLEAN', 'FEATURES', 'Enable email notifications'),
            ('data_retention_days', '365', 'NUMBER', 'DATA', 'Days to retain data')
        """);

        log.debug("All default data inserted successfully");
    }

    /**
     * Drop tenant schema (use with caution!)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dropTenantSchema(String schemaName) {
        log.warn("Dropping schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

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
        try {
            // First try with JdbcTemplate
            String query = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
            Boolean exists = jdbcTemplate.queryForObject(query, Boolean.class, schemaName);

            if (exists != null && exists) {
                log.debug("Schema {} exists (JdbcTemplate check)", schemaName);
                return true;
            }

            // Fallback to direct connection with explicit transaction handling
            return schemaExistsDirectConnection(schemaName);

        } catch (Exception e) {
            log.warn("JdbcTemplate schema check failed for {}, trying direct connection: {}", schemaName, e.getMessage());
            return schemaExistsDirectConnection(schemaName);
        }
    }

    /**
     * Direct connection check for schema existence
     */
    private boolean schemaExistsDirectConnection(String schemaName) {
        try (Connection connection = dataSource.getConnection()) {
            // Set transaction isolation to see committed changes
            connection.setAutoCommit(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            String query = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, schemaName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        boolean exists = rs.getBoolean(1);
                        log.debug("Schema {} exists (direct connection): {}", schemaName, exists);
                        return exists;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            log.error("Failed direct connection schema existence check for: {}", schemaName, e);
            return false;
        }
    }

    /**
     * Get table count in schema
     */
    public Integer getTableCount(String schemaName) {
        try {
            // First try with JdbcTemplate
            String query = """
            SELECT COUNT(*) 
            FROM information_schema.tables 
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            """;
            Integer count = jdbcTemplate.queryForObject(query, Integer.class, schemaName);

            if (count != null && count > 0) {
                log.debug("Schema {} has {} tables (JdbcTemplate)", schemaName, count);
                return count;
            }

            // Fallback to direct connection
            return getTableCountDirectConnection(schemaName);

        } catch (Exception e) {
            log.warn("JdbcTemplate table count failed for {}, trying direct connection: {}", schemaName, e.getMessage());
            return getTableCountDirectConnection(schemaName);
        }
    }

    /**
     * Direct connection check for table count
     */
    private Integer getTableCountDirectConnection(String schemaName) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            String query = """
            SELECT COUNT(*) 
            FROM information_schema.tables 
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            """;

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, schemaName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        log.debug("Schema {} has {} tables (direct connection)", schemaName, count);
                        return count;
                    }
                }
            }
            return 0;
        } catch (SQLException e) {
            log.error("Failed direct connection table count for schema: {}", schemaName, e);
            return 0;
        }
    }

    /**
     * Comprehensive verification that lists what actually exists
     */
    public boolean verifyTenantSchemaDetailed(String schemaName) {
        log.info("Detailed verification for tenant schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // Check if schema exists
            boolean schemaExists = false;
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)")) {
                stmt.setString(1, schemaName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        schemaExists = rs.getBoolean(1);
                    }
                }
            }

            if (!schemaExists) {
                log.error("Schema {} does not exist", schemaName);

                // List all schemas that do exist for debugging
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant_%' ORDER BY schema_name")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        StringBuilder existingSchemas = new StringBuilder();
                        while (rs.next()) {
                            existingSchemas.append(rs.getString(1)).append(", ");
                        }
                        log.info("Existing tenant schemas: {}", existingSchemas.toString());
                    }
                }
                return false;
            }

            // Get table count and list tables
            int tableCount = 0;
            StringBuilder tableList = new StringBuilder();
            try (PreparedStatement stmt = connection.prepareStatement("""
                SELECT table_name FROM information_schema.tables 
                WHERE table_schema = ? AND table_type = 'BASE TABLE' 
                ORDER BY table_name
                """)) {
                stmt.setString(1, schemaName);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        tableCount++;
                        tableList.append(rs.getString(1)).append(", ");
                    }
                }
            }

            log.info("Schema {} verification: {} tables found: {}", schemaName, tableCount, tableList.toString());

            if (tableCount < 6) {
                log.error("Schema {} has insufficient tables: {} (expected at least 6)", schemaName, tableCount);
                return false;
            }

            log.info("Schema {} verified successfully with {} tables", schemaName, tableCount);
            return true;

        } catch (SQLException e) {
            log.error("Failed detailed verification for schema: {}", schemaName, e);
            return false;
        }
    }

    /**
     * Get the size of a schema in bytes
     * @param schemaName The schema name
     * @return Size in bytes, or null if error
     */
    public Long getSchemaSize(String schemaName) {
        try {
            String query = """
                SELECT 
                    COALESCE(SUM(pg_total_relation_size(quote_ident(schemaname)||'.'||quote_ident(tablename))), 0) as total_size
                FROM pg_tables 
                WHERE schemaname = ?
                """;

            Long size = jdbcTemplate.queryForObject(query, Long.class, schemaName);
            log.debug("Schema {} size: {} bytes", schemaName, size);
            return size;
        } catch (Exception e) {
            log.error("Failed to get schema size for: {}", schemaName, e);
            return 0L;
        }
    }

    /**
     * Get detailed size breakdown for a schema
     * @param schemaName The schema name
     * @return Map of table names to sizes in bytes
     */
    public Map<String, Long> getSchemaTableSizes(String schemaName) {
        try {
            String query = """
                SELECT 
                    tablename,
                    pg_total_relation_size(quote_ident(schemaname)||'.'||quote_ident(tablename)) as size
                FROM pg_tables 
                WHERE schemaname = ?
                ORDER BY size DESC
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(query, schemaName);
            Map<String, Long> tableSizes = new HashMap<>();

            for (Map<String, Object> row : results) {
                String tableName = (String) row.get("tablename");
                Long size = ((Number) row.get("size")).longValue();
                tableSizes.put(tableName, size);
            }

            log.debug("Schema {} table sizes: {}", schemaName, tableSizes);
            return tableSizes;
        } catch (Exception e) {
            log.error("Failed to get table sizes for schema: {}", schemaName, e);
            return new HashMap<>();
        }
    }

    /**
     * Get schema statistics including table count, index count, and total size
     * @param schemaName The schema name
     * @return Map containing statistics
     */
    public Map<String, Object> getSchemaStatistics(String schemaName) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get table count
            stats.put("tableCount", getTableCount(schemaName));

            // Get total size
            stats.put("totalSize", getSchemaSize(schemaName));

            // Get index count
            String indexQuery = """
                SELECT COUNT(*) 
                FROM pg_indexes 
                WHERE schemaname = ?
                """;
            Integer indexCount = jdbcTemplate.queryForObject(indexQuery, Integer.class, schemaName);
            stats.put("indexCount", indexCount);

            // Get row counts for main tables
            Map<String, Long> rowCounts = new HashMap<>();
            String[] tables = {"datasets", "surveys", "models", "dashboards", "audit_logs", "tenant_settings"};

            for (String table : tables) {
                try {
                    String countQuery = String.format("SELECT COUNT(*) FROM %s.%s", schemaName, table);
                    Long count = jdbcTemplate.queryForObject(countQuery, Long.class);
                    rowCounts.put(table, count);
                } catch (Exception e) {
                    rowCounts.put(table, 0L);
                }
            }
            stats.put("rowCounts", rowCounts);

            log.info("Schema {} statistics: {}", schemaName, stats);
            return stats;
        } catch (Exception e) {
            log.error("Failed to get statistics for schema: {}", schemaName, e);
            return stats;
        }
    }
}