package com.nnipa.tenant.service;

import com.nnipa.tenant.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service for managing tenant context and schema switching.
 * Handles database connection routing and tenant isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantContextService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final TenantService tenantService;

    // Thread-local storage for current tenant context
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    /**
     * Set the current tenant context
     */
    public void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);

        // Also set the schema
        if (tenantId != null) {
            try {
                Tenant tenant = tenantService.getTenantById(java.util.UUID.fromString(tenantId));
                CURRENT_SCHEMA.set(tenant.getSchemaName());
                log.debug("Set tenant context - ID: {}, Schema: {}", tenantId, tenant.getSchemaName());
            } catch (Exception e) {
                log.error("Failed to set tenant context for ID: {}", tenantId, e);
                clearContext();
            }
        }
    }

    /**
     * Set tenant context by subdomain
     */
    public void setCurrentTenantBySubdomain(String subdomain) {
        try {
            Tenant tenant = tenantService.getTenantBySubdomain(subdomain);
            CURRENT_TENANT.set(tenant.getId().toString());
            CURRENT_SCHEMA.set(tenant.getSchemaName());
            log.debug("Set tenant context by subdomain - Subdomain: {}, Schema: {}", subdomain, tenant.getSchemaName());
        } catch (Exception e) {
            log.error("Failed to set tenant context for subdomain: {}", subdomain, e);
            clearContext();
        }
    }

    /**
     * Get the current tenant ID
     */
    public String getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Get the current tenant schema
     */
    public String getCurrentSchema() {
        return CURRENT_SCHEMA.get();
    }

    /**
     * Clear the tenant context
     */
    public void clearContext() {
        CURRENT_TENANT.remove();
        CURRENT_SCHEMA.remove();
        log.debug("Cleared tenant context");
    }

    /**
     * Execute a query in tenant schema context
     */
    public <T> T executeInTenantSchema(String schemaName, TenantSchemaCallback<T> callback) {
        log.debug("Executing in tenant schema: {}", schemaName);

        try (Connection connection = dataSource.getConnection()) {
            // Save current schema
            String originalSchema = getCurrentSearchPath(connection);

            try {
                // Switch to tenant schema
                setSearchPath(connection, schemaName);

                // Execute callback
                return callback.execute(connection);

            } finally {
                // Restore original schema
                if (originalSchema != null) {
                    setSearchPath(connection, originalSchema);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to execute in tenant schema: {}", schemaName, e);
            throw new RuntimeException("Failed to execute in tenant schema", e);
        }
    }

    /**
     * Execute a JdbcTemplate operation in tenant context
     */
    public <T> T executeWithTenantJdbc(String schemaName, TenantJdbcCallback<T> callback) {
        log.debug("Executing JDBC operation in tenant schema: {}", schemaName);

        // Set search path for current connection
        jdbcTemplate.execute("SET search_path TO " + schemaName);

        try {
            return callback.execute(jdbcTemplate);
        } finally {
            // Reset to default
            jdbcTemplate.execute("SET search_path TO tenant_registry, public");
        }
    }

    /**
     * Get connection for specific tenant
     */
    public Connection getTenantConnection(String schemaName) throws SQLException {
        Connection connection = dataSource.getConnection();
        setSearchPath(connection, schemaName);
        return connection;
    }

    /**
     * Check if current user has access to tenant
     */
    public boolean hasAccessToTenant(String userId, String tenantId) {
        // TODO: Implement actual access control logic
        // This would check user-tenant mappings, roles, etc.
        log.debug("Checking access for user {} to tenant {}", userId, tenantId);
        return true; // Placeholder
    }

    /**
     * Validate tenant context is set
     */
    public void validateTenantContext() {
        if (getCurrentTenantId() == null || getCurrentSchema() == null) {
            throw new IllegalStateException("No tenant context set");
        }
    }

    // ========== Helper Methods ==========

    private String getCurrentSearchPath(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SHOW search_path")) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    private void setSearchPath(Connection connection, String schema) throws SQLException {
        String sql = String.format("SET search_path TO %s, public", schema);
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * Callback interface for tenant schema operations
     */
    @FunctionalInterface
    public interface TenantSchemaCallback<T> {
        T execute(Connection connection) throws SQLException;
    }

    /**
     * Callback interface for tenant JDBC operations
     */
    @FunctionalInterface
    public interface TenantJdbcCallback<T> {
        T execute(JdbcTemplate jdbcTemplate);
    }
}