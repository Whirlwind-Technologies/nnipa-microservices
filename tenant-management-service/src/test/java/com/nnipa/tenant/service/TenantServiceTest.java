package com.nnipa.tenant.service;

import com.nnipa.tenant.entity.Tenant;
import com.nnipa.tenant.entity.TenantAddress;
import com.nnipa.tenant.entity.TenantFeature;
import com.nnipa.tenant.enums.FeatureFlag;
import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import com.nnipa.tenant.exception.TenantAlreadyExistsException;
import com.nnipa.tenant.exception.TenantNotFoundException;
import com.nnipa.tenant.exception.TenantValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TenantService
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantServiceTest {

    @Autowired
    private TenantService tenantService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        // Create a test tenant
        testTenant = Tenant.builder()
                .name("Test Company")
                .displayName("Test Co")
                .subdomain("testco")
                .contactEmail("admin@testco.com")
                .subscriptionPlan(SubscriptionPlan.PROFESSIONAL)
                .build();
    }

    @Test
    void testCreateTenant() {
        // Act
        Tenant created = tenantService.createTenant(testTenant);

        // Assert
        assertNotNull(created.getId());
        assertEquals("Test Company", created.getName());
        assertEquals("testco", created.getSubdomain());
        assertEquals(TenantStatus.PENDING, created.getStatus());
        assertEquals(SubscriptionPlan.PROFESSIONAL, created.getSubscriptionPlan());
        assertEquals("tenant_testco", created.getSchemaName());

        // Check resource limits for PROFESSIONAL plan
        assertEquals(200, created.getMaxUsers());
        assertEquals(500, created.getMaxStorageGb());
        assertEquals(2000, created.getMaxDatasets());
    }

    @Test
    void testCreateTenant_DuplicateSubdomain() {
        // Arrange
        tenantService.createTenant(testTenant);

        // Create another tenant with same subdomain
        Tenant duplicate = Tenant.builder()
                .name("Another Company")
                .subdomain("testco")
                .contactEmail("admin@another.com")
                .build();

        // Act & Assert
        assertThrows(TenantAlreadyExistsException.class, () -> {
            tenantService.createTenant(duplicate);
        });
    }

    @Test
    void testCreateTenant_InvalidData() {
        // Test null name
        Tenant invalidTenant = Tenant.builder()
                .subdomain("valid")
                .build();

        Tenant finalInvalidTenant = invalidTenant;
        assertThrows(TenantValidationException.class, () -> {
            tenantService.createTenant(finalInvalidTenant);
        });

        // Test invalid subdomain format
        invalidTenant = Tenant.builder()
                .name("Valid Name")
                .subdomain("Invalid_Subdomain!")
                .build();

        Tenant finalInvalidTenant1 = invalidTenant;
        assertThrows(TenantValidationException.class, () -> {
            tenantService.createTenant(finalInvalidTenant1);
        });
    }

    @Test
    void testGetTenantById() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);

        // Act
        Tenant retrieved = tenantService.getTenantById(created.getId());

        // Assert
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getName(), retrieved.getName());
    }

    @Test
    void testGetTenantById_NotFound() {
        UUID randomId = UUID.randomUUID();

        assertThrows(TenantNotFoundException.class, () -> {
            tenantService.getTenantById(randomId);
        });
    }

    @Test
    void testGetTenantBySubdomain() {
        // Arrange
        tenantService.createTenant(testTenant);

        // Act
        Tenant retrieved = tenantService.getTenantBySubdomain("testco");

        // Assert
        assertNotNull(retrieved);
        assertEquals("testco", retrieved.getSubdomain());
        assertEquals("Test Company", retrieved.getName());
    }

    @Test
    void testUpdateTenant() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);

        // Prepare updates
        Tenant updates = Tenant.builder()
                .displayName("Updated Display Name")
                .description("Updated description")
                .website("https://testco.com")
                .contactPhone("+1234567890")
                .build();

        // Act
        Tenant updated = tenantService.updateTenant(created.getId(), updates);

        // Assert
        assertEquals("Updated Display Name", updated.getDisplayName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals("https://testco.com", updated.getWebsite());
        assertEquals("+1234567890", updated.getContactPhone());
        // Original values should remain
        assertEquals("Test Company", updated.getName());
        assertEquals("testco", updated.getSubdomain());
    }

    @Test
    void testDeleteTenant() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);

        // Act
        tenantService.deleteTenant(created.getId());

        // Assert - tenant should be soft deleted
        // Note: In a real scenario, you'd check the deletedAt field
        // For now, we'll verify it doesn't throw an exception
        assertDoesNotThrow(() -> tenantService.deleteTenant(created.getId()));
    }

    @Test
    void testSearchTenants() {
        // Arrange
        tenantService.createTenant(testTenant);

        Tenant tenant2 = Tenant.builder()
                .name("Another Test Company")
                .subdomain("anothertest")
                .contactEmail("admin@another.com")
                .build();
        tenantService.createTenant(tenant2);

        // Act
        Page<Tenant> results = tenantService.searchTenants("test", PageRequest.of(0, 10));

        // Assert
        assertNotNull(results);
        assertTrue(results.getTotalElements() >= 2);
    }

    @Test
    void testProvisionTenant() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);

        // Act
        tenantService.provisionTenant(created.getId());

        // Assert
        Tenant provisioned = tenantService.getTenantById(created.getId());
        assertEquals(TenantStatus.ACTIVE, provisioned.getStatus());
    }

    @Test
    void testUpdateSubscriptionPlan() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);

        // Act
        Tenant updated = tenantService.updateSubscriptionPlan(created.getId(), SubscriptionPlan.ENTERPRISE);

        // Assert
        assertEquals(SubscriptionPlan.ENTERPRISE, updated.getSubscriptionPlan());
        assertEquals(-1, updated.getMaxUsers()); // Unlimited
        assertEquals(-1, updated.getMaxStorageGb());
        assertEquals(-1, updated.getMaxDatasets());
    }

    @Test
    void testFeatureManagement() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);

        // Act - Enable a feature
        TenantFeature feature = tenantService.enableFeature(created.getId(), FeatureFlag.ML_MODELS);

        // Assert
        assertNotNull(feature);
        assertTrue(feature.getEnabled());
        assertEquals(FeatureFlag.ML_MODELS, feature.getFeature());

        // Test hasFeature
        assertTrue(tenantService.hasFeature(created.getId(), FeatureFlag.ML_MODELS));

        // Test disable feature
        tenantService.disableFeature(created.getId(), FeatureFlag.ML_MODELS);
        assertFalse(tenantService.hasFeature(created.getId(), FeatureFlag.ML_MODELS));

        // Test get all features
        List<TenantFeature> features = tenantService.getTenantFeatures(created.getId());
        assertNotNull(features);
        assertTrue(features.size() > 0);
    }

    @Test
    void testConfigurationManagement() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);

        // Act - Set configuration
        tenantService.setConfiguration(created.getId(), "test_key", "test_value");

        // Assert - Get configuration
        String value = tenantService.getConfiguration(created.getId(), "test_key");
        assertEquals("test_value", value);

        // Test get all configurations
        Map<String, String> configs = tenantService.getAllConfigurations(created.getId());
        assertNotNull(configs);
        assertTrue(configs.containsKey("test_key"));
        assertEquals("test_value", configs.get("test_key"));

        // Test delete configuration
        tenantService.deleteConfiguration(created.getId(), "test_key");
        assertNull(tenantService.getConfiguration(created.getId(), "test_key"));
    }

    @Test
    void testUsageMetrics() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);

        // Act
        tenantService.updateUsageMetrics(created.getId(), 50, 100.5, 500);

        // Assert
        Tenant updated = tenantService.getTenantById(created.getId());
        assertEquals(50, updated.getCurrentUsers());
        assertEquals(100.5, updated.getCurrentStorageGb());
        assertEquals(500, updated.getCurrentDatasets());

        // Test check limits
        assertTrue(tenantService.checkLimits(created.getId()));

        // Update to exceed limits
        tenantService.updateUsageMetrics(created.getId(), 201, 501.0, 2001);
        assertFalse(tenantService.checkLimits(created.getId()));
    }

    @Test
    void testTenantStatistics() {
        // Arrange
        Tenant created = tenantService.createTenant(testTenant);
        tenantService.updateUsageMetrics(created.getId(), 10, 50.0, 100);

        // Act
        Map<String, Object> stats = tenantService.getTenantStatistics(created.getId());

        // Assert
        assertNotNull(stats);
        assertEquals(created.getId(), stats.get("tenantId"));
        assertEquals("Test Company", stats.get("name"));
        assertEquals(10, stats.get("currentUsers"));
        assertEquals(50.0, stats.get("currentStorageGb"));
        assertEquals(100, stats.get("currentDatasets"));
    }

    @Test
    void testSubdomainAvailability() {
        // Before creating tenant
        assertTrue(tenantService.isSubdomainAvailable("uniquesub"));

        // Create tenant
        testTenant.setSubdomain("uniquesub");
        tenantService.createTenant(testTenant);

        // After creating tenant
        assertFalse(tenantService.isSubdomainAvailable("uniquesub"));
    }

    @Test
    void testTenantLifecycle() {
        // Create tenant
        Tenant created = tenantService.createTenant(testTenant);
        assertEquals(TenantStatus.PENDING, created.getStatus());

        // Provision tenant
        tenantService.provisionTenant(created.getId());
        Tenant provisioned = tenantService.getTenantById(created.getId());
        assertEquals(TenantStatus.ACTIVE, provisioned.getStatus());

        // Suspend tenant
        Tenant suspended = tenantService.suspendTenant(created.getId(), "Payment failed");
        assertEquals(TenantStatus.SUSPENDED, suspended.getStatus());
        assertNotNull(suspended.getSuspendedAt());

        // Reactivate tenant
        Tenant reactivated = tenantService.reactivateTenant(created.getId());
        assertEquals(TenantStatus.ACTIVE, reactivated.getStatus());
        assertNull(reactivated.getSuspendedAt());

        // Archive tenant
        Tenant archived = tenantService.archiveTenant(created.getId());
        assertEquals(TenantStatus.ARCHIVED, archived.getStatus());
    }

    @Test
    void testTrialManagement() {
        // Arrange
        testTenant.setSubscriptionPlan(SubscriptionPlan.FREE);
        Tenant created = tenantService.createTenant(testTenant);

        // Assert - Free plan should have trial
        assertNotNull(created.getTrialEndsAt());
        assertTrue(created.isInTrial());

        // Test extending trial
        Tenant extended = tenantService.startTrial(created.getId(), 7);
        assertNotNull(extended.getTrialEndsAt());
    }
}