package com.nnipa.tenant.service;

import com.nnipa.tenant.entity.Tenant;
import com.nnipa.tenant.entity.TenantConfiguration;
import com.nnipa.tenant.entity.TenantFeature;
import com.nnipa.tenant.enums.FeatureFlag;
import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for tenant management operations.
 * Defines all business operations related to tenants.
 */
public interface TenantService {

    // ========== Tenant CRUD Operations ==========

    /**
     * Create a new tenant
     */
    Tenant createTenant(Tenant tenant);

    /**
     * Get tenant by ID
     */
    Tenant getTenantById(UUID tenantId);

    /**
     * Get tenant by subdomain
     */
    Tenant getTenantBySubdomain(String subdomain);

    /**
     * Update tenant information
     */
    Tenant updateTenant(UUID tenantId, Tenant tenant);

    /**
     * Delete tenant (soft delete)
     */
    void deleteTenant(UUID tenantId);

    /**
     * Search tenants
     */
    Page<Tenant> searchTenants(String searchTerm, Pageable pageable);

    /**
     * Get all tenants
     */
    Page<Tenant> getAllTenants(Pageable pageable);

    /**
     * Get tenants by status
     */
    List<Tenant> getTenantsByStatus(TenantStatus status);

    // ========== Tenant Lifecycle Operations ==========

    /**
     * Provision a new tenant (create schema, initialize data)
     */
    void provisionTenant(UUID tenantId);

    /**
     * Activate a tenant
     */
    Tenant activateTenant(UUID tenantId);

    /**
     * Suspend a tenant
     */
    Tenant suspendTenant(UUID tenantId, String reason);

    /**
     * Reactivate a suspended tenant
     */
    Tenant reactivateTenant(UUID tenantId);

    /**
     * Archive a tenant
     */
    Tenant archiveTenant(UUID tenantId);

    // ========== Subscription Operations ==========

    /**
     * Update tenant subscription plan
     */
    Tenant updateSubscriptionPlan(UUID tenantId, SubscriptionPlan newPlan);

    /**
     * Start trial for tenant
     */
    Tenant startTrial(UUID tenantId, Integer trialDays);

    /**
     * Extend subscription
     */
    Tenant extendSubscription(UUID tenantId, Integer days);

    /**
     * Cancel subscription
     */
    void cancelSubscription(UUID tenantId, String reason);

    // ========== Feature Management ==========

    /**
     * Enable feature for tenant
     */
    TenantFeature enableFeature(UUID tenantId, FeatureFlag feature);

    /**
     * Disable feature for tenant
     */
    void disableFeature(UUID tenantId, FeatureFlag feature);

    /**
     * Get all features for tenant
     */
    List<TenantFeature> getTenantFeatures(UUID tenantId);

    /**
     * Check if tenant has feature
     */
    boolean hasFeature(UUID tenantId, FeatureFlag feature);

    /**
     * Update features based on subscription plan
     */
    void updateFeaturesForPlan(UUID tenantId, SubscriptionPlan plan);

    // ========== Configuration Management ==========

    /**
     * Set configuration value
     */
    TenantConfiguration setConfiguration(UUID tenantId, String key, String value);

    /**
     * Get configuration value
     */
    String getConfiguration(UUID tenantId, String key);

    /**
     * Get all configurations for tenant
     */
    Map<String, String> getAllConfigurations(UUID tenantId);

    /**
     * Delete configuration
     */
    void deleteConfiguration(UUID tenantId, String key);

    // ========== Usage and Limits ==========

    /**
     * Update usage metrics
     */
    void updateUsageMetrics(UUID tenantId, Integer users, Double storageGb, Integer datasets);

    /**
     * Check tenant limits
     */
    boolean checkLimits(UUID tenantId);

    /**
     * Get tenant statistics
     */
    Map<String, Object> getTenantStatistics(UUID tenantId);

    /**
     * Get platform statistics
     */
    Map<String, Object> getPlatformStatistics();

    // ========== Validation ==========

    /**
     * Validate tenant subdomain
     */
    boolean isSubdomainAvailable(String subdomain);

    /**
     * Validate tenant data
     */
    void validateTenant(Tenant tenant);
}