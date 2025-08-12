package com.nnipa.tenant.entity;

import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test to verify entity creation and business logic
 */
@SpringBootTest
@ActiveProfiles("test")
class TenantEntityTest {

    @Test
    void testTenantEntityCreation() {
        // Create a new tenant
        Tenant tenant = Tenant.builder()
                .name("Test Organization")
                .displayName("Test Org")
                .subdomain("test")
                .schemaName("tenant_test")
                .status(TenantStatus.ACTIVE)
                .subscriptionPlan(SubscriptionPlan.PROFESSIONAL)
                .contactEmail("test@example.com")
                .maxUsers(100)
                .maxStorageGb(250)
                .maxDatasets(1000)
                .currentUsers(10)
                .currentStorageGb(50.0)
                .currentDatasets(100)
                .build();

        // Verify tenant properties
        assertNotNull(tenant);
        assertEquals("Test Organization", tenant.getName());
        assertEquals("test", tenant.getSubdomain());
        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        assertEquals(SubscriptionPlan.PROFESSIONAL, tenant.getSubscriptionPlan());

        // Test business methods
        assertTrue(tenant.isActive());
        assertFalse(tenant.hasReachedUserLimit());
        assertFalse(tenant.hasReachedStorageLimit());
        assertFalse(tenant.hasReachedDatasetLimit());
    }

    @Test
    void testTenantLimits() {
        Tenant tenant = Tenant.builder()
                .name("Limited Tenant")
                .subdomain("limited")
                .schemaName("tenant_limited")
                .status(TenantStatus.ACTIVE)
                .maxUsers(10)
                .currentUsers(10)
                .maxStorageGb(100)
                .currentStorageGb(100.0)
                .maxDatasets(50)
                .currentDatasets(50)
                .build();

        // All limits should be reached
        assertTrue(tenant.hasReachedUserLimit());
        assertTrue(tenant.hasReachedStorageLimit());
        assertTrue(tenant.hasReachedDatasetLimit());
    }

    @Test
    void testTenantTrialPeriod() {
        Tenant tenant = Tenant.builder()
                .name("Trial Tenant")
                .subdomain("trial")
                .schemaName("tenant_trial")
                .status(TenantStatus.ACTIVE)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .trialEndsAt(Instant.now().plusSeconds(86400)) // 1 day from now
                .build();

        assertTrue(tenant.isInTrial());

        // Set trial to past
        tenant.setTrialEndsAt(Instant.now().minusSeconds(86400));
        assertFalse(tenant.isInTrial());
    }

    @Test
    void testSubscriptionPlanFeatures() {
        // Test paid plan
        assertTrue(SubscriptionPlan.PROFESSIONAL.isPaid());
        assertFalse(SubscriptionPlan.FREE.isPaid());

        // Test trial period
        assertTrue(SubscriptionPlan.FREE.hasTrial());
        assertEquals(30, SubscriptionPlan.FREE.getTrialDays());

        // Test plan lookup
        assertEquals(SubscriptionPlan.ENTERPRISE,
                SubscriptionPlan.fromString("enterprise"));
    }

    @Test
    void testTenantAddress() {
        TenantAddress address = TenantAddress.builder()
                .addressLine1("123 Main St")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .country("US")
                .build();

        String formatted = address.getFormattedAddress();
        assertNotNull(formatted);
        assertTrue(formatted.contains("123 Main St"));
        assertTrue(formatted.contains("New York"));
    }
}