package com.nnipa.tenant.service;

import com.nnipa.tenant.entity.Tenant;
import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for tenant provisioning functionality.
 * Tests the creation of tenant schemas and core business tables.
 *
 * NOTE: User-related tables are NOT created here - they are managed
 * by the User Management Service in a microservices architecture.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantProvisioningTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private DatabaseProvisioningService databaseProvisioningService;

    @Autowired
    private TenantAsyncService tenantAsyncService;

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        // Create a test tenant
        testTenant = Tenant.builder()
                .name("Provisioning Test Company")
                .displayName("Provision Test")
                .subdomain("provtest")
                .contactEmail("admin@provtest.com")
                .subscriptionPlan(SubscriptionPlan.PROFESSIONAL)
                .build();
    }

    @Test
    void testFullTenantProvisioning() {
        // Create tenant
        Tenant created = tenantService.createTenant(testTenant);
        assertNotNull(created.getId());
        assertEquals(TenantStatus.PENDING, created.getStatus());
        assertEquals("tenant_provtest", created.getSchemaName());

        // Provision tenant
        tenantService.provisionTenant(created.getId());

        // Verify tenant status updated
        Tenant provisioned = tenantService.getTenantById(created.getId());
        assertEquals(TenantStatus.ACTIVE, provisioned.getStatus());

        // Verify schema exists
        assertTrue(databaseProvisioningService.schemaExists(provisioned.getSchemaName()));

        // Verify tables created (should be at least 6 core tables)
        Integer tableCount = databaseProvisioningService.getTableCount(provisioned.getSchemaName());
        assertTrue(tableCount >= 6, "Should have at least 6 core tables");

        // Verify default data inserted
        String schema = provisioned.getSchemaName();
        tenantContextService.executeWithTenantJdbc(schema, jdbc -> {
            // NOTE: We do NOT check for users table - that's User Management Service responsibility

            // Check dashboards table with default dashboard
            Integer dashboardCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM dashboards", Integer.class
            );
            assertTrue(dashboardCount > 0, "Should have at least one default dashboard");

            // Check datasets table exists (may be empty initially)
            Integer datasetCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM datasets", Integer.class
            );
            assertNotNull(datasetCount, "Datasets table should exist");
            // Note: datasets may be 0 initially if no default data is inserted

            // Check surveys table exists (may be empty initially)
            Integer surveyCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM surveys", Integer.class
            );
            assertNotNull(surveyCount, "Surveys table should exist");

            // Check models table exists (may be empty initially)
            Integer modelCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM models", Integer.class
            );
            assertNotNull(modelCount, "Models table should exist");

            // Check tenant_settings table with default settings
            Integer settingsCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM tenant_settings", Integer.class
            );
            assertTrue(settingsCount > 0, "Should have default tenant settings");

            // Check audit logs for provisioning event
            Integer auditCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM audit_logs WHERE action = 'TENANT_PROVISIONED'",
                    Integer.class
            );
            assertTrue(auditCount > 0, "Should have tenant provisioning audit log");

            return null;
        });
    }

    @Test
    void testAsyncTenantProvisioning() throws Exception {
        // Create tenant
        Tenant created = tenantService.createTenant(testTenant);

        // Provision asynchronously
        CompletableFuture<Void> future = tenantAsyncService.provisionTenantAsync(created.getId());

        // Wait for completion
        future.get(); // This will throw if provisioning failed

        // Verify provisioning succeeded
        Tenant provisioned = tenantService.getTenantById(created.getId());
        assertEquals(TenantStatus.ACTIVE, provisioned.getStatus());

        // Verify provisioning
        CompletableFuture<Boolean> verifyFuture = tenantAsyncService.verifyTenantProvisioningAsync(created.getId());
        Boolean isValid = verifyFuture.get();
        assertTrue(isValid);
    }

    @Test
    void testProvisioningRollback() {
        // Create tenant with invalid schema name to force failure
        testTenant.setSchemaName("123-invalid-schema!"); // Invalid characters
        Tenant created = tenantService.createTenant(testTenant);

        // Attempt provisioning (should fail)
        assertThrows(Exception.class, () -> {
            tenantService.provisionTenant(created.getId());
        });

        // Verify tenant status rolled back
        Tenant tenant = tenantService.getTenantById(created.getId());
        assertEquals(TenantStatus.PENDING, tenant.getStatus());

        // Verify schema was not created
        assertFalse(databaseProvisioningService.schemaExists("123-invalid-schema!"));
    }

    @Test
    void testTenantContextSwitching() {
        // Create and provision first tenant
        Tenant tenant1 = tenantService.createTenant(testTenant);
        tenantService.provisionTenant(tenant1.getId());

        // Create and provision second tenant
        Tenant tenant2 = Tenant.builder()
                .name("Second Test Company")
                .subdomain("second")
                .contactEmail("admin@second.com")
                .build();
        tenant2 = tenantService.createTenant(tenant2);
        tenantService.provisionTenant(tenant2.getId());

        // Test context switching
        tenantContextService.setCurrentTenant(tenant1.getId().toString());
        assertEquals(tenant1.getId().toString(), tenantContextService.getCurrentTenantId());
        assertEquals(tenant1.getSchemaName(), tenantContextService.getCurrentSchema());

        // Switch to tenant2
        tenantContextService.setCurrentTenantBySubdomain("second");
        assertEquals(tenant2.getId().toString(), tenantContextService.getCurrentTenantId());
        assertEquals(tenant2.getSchemaName(), tenantContextService.getCurrentSchema());

        // Clear context
        tenantContextService.clearContext();
        assertNull(tenantContextService.getCurrentTenantId());
        assertNull(tenantContextService.getCurrentSchema());
    }

    @Test
    void testSchemaIsolation() {
        // Create and provision two tenants
        Tenant tenant1 = tenantService.createTenant(testTenant);
        tenantService.provisionTenant(tenant1.getId());

        Tenant tenant2 = Tenant.builder()
                .name("Isolated Test Company")
                .subdomain("isolated")
                .contactEmail("admin@isolated.com")
                .build();
        tenant2 = tenantService.createTenant(tenant2);
        tenantService.provisionTenant(tenant2.getId());

        // Insert data in tenant1 schema
        tenantContextService.executeWithTenantJdbc(tenant1.getSchemaName(), jdbc -> {
            jdbc.update(
                    "INSERT INTO datasets (name, description) VALUES (?, ?)",
                    "Tenant1 Dataset", "Data for tenant 1"
            );
            return null;
        });

        // Insert data in tenant2 schema
        tenantContextService.executeWithTenantJdbc(tenant2.getSchemaName(), jdbc -> {
            jdbc.update(
                    "INSERT INTO datasets (name, description) VALUES (?, ?)",
                    "Tenant2 Dataset", "Data for tenant 2"
            );
            return null;
        });

        // Verify isolation - tenant1 should only see its data
        tenantContextService.executeWithTenantJdbc(tenant1.getSchemaName(), jdbc -> {
            List<Map<String, Object>> datasets = jdbc.queryForList(
                    "SELECT name FROM datasets"
            );

            // Should have 1 dataset (the one we inserted - no default datasets)
            assertEquals(1, datasets.size());

            // Verify we only see tenant1's data
            boolean hasTenant1Data = datasets.stream()
                    .anyMatch(d -> "Tenant1 Dataset".equals(d.get("name")));
            boolean hasTenant2Data = datasets.stream()
                    .anyMatch(d -> "Tenant2 Dataset".equals(d.get("name")));

            assertTrue(hasTenant1Data);
            assertFalse(hasTenant2Data); // Should NOT see tenant2's data

            return null;
        });

        // Verify isolation - tenant2 should only see its data
        tenantContextService.executeWithTenantJdbc(tenant2.getSchemaName(), jdbc -> {
            List<Map<String, Object>> datasets = jdbc.queryForList(
                    "SELECT name FROM datasets"
            );

            // Should have 1 dataset (the one we inserted - no default datasets)
            assertEquals(1, datasets.size());

            // Verify we only see tenant2's data
            boolean hasTenant1Data = datasets.stream()
                    .anyMatch(d -> "Tenant1 Dataset".equals(d.get("name")));
            boolean hasTenant2Data = datasets.stream()
                    .anyMatch(d -> "Tenant2 Dataset".equals(d.get("name")));

            assertFalse(hasTenant1Data); // Should NOT see tenant1's data
            assertTrue(hasTenant2Data);

            return null;
        });
    }

    @Test
    void testCoreBusinessTables() {
        // Create and provision tenant
        Tenant tenant = tenantService.createTenant(testTenant);
        tenantService.provisionTenant(tenant.getId());

        // Verify all core business tables exist
        tenantContextService.executeWithTenantJdbc(tenant.getSchemaName(), jdbc -> {
            // Check datasets table structure
            List<Map<String, Object>> datasetColumns = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns " +
                            "WHERE table_schema = ? AND table_name = 'datasets'",
                    tenant.getSchemaName()
            );
            assertFalse(datasetColumns.isEmpty(), "Datasets table should have columns");

            // Check surveys table structure
            List<Map<String, Object>> surveyColumns = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns " +
                            "WHERE table_schema = ? AND table_name = 'surveys'",
                    tenant.getSchemaName()
            );
            assertFalse(surveyColumns.isEmpty(), "Surveys table should have columns");

            // Check models table structure
            List<Map<String, Object>> modelColumns = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns " +
                            "WHERE table_schema = ? AND table_name = 'models'",
                    tenant.getSchemaName()
            );
            assertFalse(modelColumns.isEmpty(), "Models table should have columns");

            // Check dashboards table structure
            List<Map<String, Object>> dashboardColumns = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns " +
                            "WHERE table_schema = ? AND table_name = 'dashboards'",
                    tenant.getSchemaName()
            );
            assertFalse(dashboardColumns.isEmpty(), "Dashboards table should have columns");

            // Check audit_logs table structure
            List<Map<String, Object>> auditColumns = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns " +
                            "WHERE table_schema = ? AND table_name = 'audit_logs'",
                    tenant.getSchemaName()
            );
            assertFalse(auditColumns.isEmpty(), "Audit logs table should have columns");

            // Check tenant_settings table structure
            List<Map<String, Object>> settingsColumns = jdbc.queryForList(
                    "SELECT column_name FROM information_schema.columns " +
                            "WHERE table_schema = ? AND table_name = 'tenant_settings'",
                    tenant.getSchemaName()
            );
            assertFalse(settingsColumns.isEmpty(), "Tenant settings table should have columns");

            return null;
        });
    }

    @Test
    void testSchemaSize() {
        // Create and provision tenant
        Tenant tenant = tenantService.createTenant(testTenant);
        tenantService.provisionTenant(tenant.getId());

        // Get initial schema size
        Long initialSize = databaseProvisioningService.getSchemaSize(tenant.getSchemaName());
        assertNotNull(initialSize);
        assertTrue(initialSize > 0);

        // Add some data
        tenantContextService.executeWithTenantJdbc(tenant.getSchemaName(), jdbc -> {
            for (int i = 0; i < 10; i++) {
                jdbc.update(
                        "INSERT INTO datasets (name, description, size_bytes) VALUES (?, ?, ?)",
                        "Dataset " + i, "Test dataset " + i, 1000000L
                );
            }
            return null;
        });

        // Get new schema size
        Long newSize = databaseProvisioningService.getSchemaSize(tenant.getSchemaName());
        assertNotNull(newSize);

        // Size should have increased
        assertTrue(newSize >= initialSize);
    }

    @Test
    void testTenantLifecycleComplete() {
        // Create tenant
        Tenant created = tenantService.createTenant(testTenant);
        assertEquals(TenantStatus.PENDING, created.getStatus());

        // Provision
        tenantService.provisionTenant(created.getId());
        Tenant provisioned = tenantService.getTenantById(created.getId());
        assertEquals(TenantStatus.ACTIVE, provisioned.getStatus());

        // Suspend
        Tenant suspended = tenantService.suspendTenant(created.getId(), "Test suspension");
        assertEquals(TenantStatus.SUSPENDED, suspended.getStatus());

        // Reactivate
        Tenant reactivated = tenantService.reactivateTenant(created.getId());
        assertEquals(TenantStatus.ACTIVE, reactivated.getStatus());

        // Archive
        Tenant archived = tenantService.archiveTenant(created.getId());
        assertEquals(TenantStatus.ARCHIVED, archived.getStatus());

        // Verify schema still exists (archived, not deleted)
        assertTrue(databaseProvisioningService.schemaExists(archived.getSchemaName()));
    }

    @Test
    void testDefaultDataProvisioning() {
        // Create and provision tenant
        Tenant tenant = tenantService.createTenant(testTenant);
        tenantService.provisionTenant(tenant.getId());

        tenantContextService.executeWithTenantJdbc(tenant.getSchemaName(), jdbc -> {
            // Verify default dashboard was created
            List<Map<String, Object>> dashboards = jdbc.queryForList(
                    "SELECT name, description, type FROM dashboards WHERE name = 'Welcome Dashboard'"
            );
            assertEquals(1, dashboards.size(), "Should have default welcome dashboard");

            // Verify default settings were created
            List<Map<String, Object>> settings = jdbc.queryForList(
                    "SELECT key, value FROM tenant_settings ORDER BY key"
            );
            assertTrue(settings.size() >= 5, "Should have at least 5 default settings");

            // Check specific default settings
            boolean hasDateFormat = settings.stream()
                    .anyMatch(s -> "date_format".equals(s.get("key")));
            boolean hasTimezone = settings.stream()
                    .anyMatch(s -> "time_zone".equals(s.get("key")));
            boolean hasLanguage = settings.stream()
                    .anyMatch(s -> "language".equals(s.get("key")));

            assertTrue(hasDateFormat, "Should have date_format setting");
            assertTrue(hasTimezone, "Should have time_zone setting");
            assertTrue(hasLanguage, "Should have language setting");

            return null;
        });
    }
}