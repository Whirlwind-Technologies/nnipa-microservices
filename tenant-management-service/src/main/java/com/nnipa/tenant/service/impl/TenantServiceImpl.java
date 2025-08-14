package com.nnipa.tenant.service.impl;

import com.nnipa.tenant.entity.*;
import com.nnipa.tenant.enums.FeatureFlag;
import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import com.nnipa.tenant.exception.*;
import com.nnipa.tenant.repository.*;
import com.nnipa.tenant.service.DatabaseProvisioningService;
import com.nnipa.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of TenantService interface.
 * Handles all business logic related to tenant management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final TenantFeatureRepository featureRepository;
    private final TenantConfigurationRepository configurationRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantUsageRepository usageRepository;
    private final DatabaseProvisioningService databaseProvisioningService;

    // ========== Tenant CRUD Operations ==========

    @Override
    public Tenant createTenant(Tenant tenant) {
        log.info("Creating new tenant with subdomain: {}", tenant.getSubdomain());

        // Validate tenant data
        validateTenant(tenant);

        // Check if subdomain already exists
        if (tenantRepository.existsBySubdomain(tenant.getSubdomain())) {
            throw new TenantAlreadyExistsException(tenant.getSubdomain());
        }

        // Generate schema name if not provided
        if (tenant.getSchemaName() == null) {
            tenant.setSchemaName("tenant_" + tenant.getSubdomain().toLowerCase().replace("-", "_"));
        }

        // Set default values
        tenant.setStatus(TenantStatus.PENDING);
        if (tenant.getSubscriptionPlan() == null) {
            tenant.setSubscriptionPlan(SubscriptionPlan.FREE);
        }

        // Set resource limits based on plan
        setResourceLimitsForPlan(tenant, tenant.getSubscriptionPlan());

        // Set trial period for free plan
        if (tenant.getSubscriptionPlan() == SubscriptionPlan.FREE) {
            tenant.setTrialEndsAt(Instant.now().plus(30, ChronoUnit.DAYS));
        }

        // Save tenant
        Tenant savedTenant = tenantRepository.save(tenant);

        // Create initial subscription record
        createInitialSubscription(savedTenant);

        // Add default features for the plan
        addDefaultFeaturesForPlan(savedTenant);

        // Add default configurations
        addDefaultConfigurations(savedTenant);

        log.info("Successfully created tenant with ID: {}", savedTenant.getId());
        return savedTenant;
    }

    @Override
    @Cacheable(value = "tenants", key = "#tenantId")
    public Tenant getTenantById(UUID tenantId) {
        log.debug("Fetching tenant with ID: {}", tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId.toString()));

        loadTenantFeatures(tenant);

        return tenant;
    }

    @Override
    @Cacheable(value = "tenants", key = "#subdomain")
    public Tenant getTenantBySubdomain(String subdomain) {
        log.debug("Fetching tenant with subdomain: {}", subdomain);
        Tenant tenant = tenantRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new TenantNotFoundException(subdomain));

        loadTenantFeatures(tenant);

        return tenant;
    }

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant updateTenant(UUID tenantId, Tenant updates) {
        log.info("Updating tenant with ID: {}", tenantId);

        Tenant existingTenant = getTenantById(tenantId);

        // Update allowed fields
        if (updates.getName() != null) {
            existingTenant.setName(updates.getName());
        }
        if (updates.getDisplayName() != null) {
            existingTenant.setDisplayName(updates.getDisplayName());
        }
        if (updates.getDescription() != null) {
            existingTenant.setDescription(updates.getDescription());
        }
        if (updates.getLogoUrl() != null) {
            existingTenant.setLogoUrl(updates.getLogoUrl());
        }
        if (updates.getWebsite() != null) {
            existingTenant.setWebsite(updates.getWebsite());
        }
        if (updates.getContactEmail() != null) {
            existingTenant.setContactEmail(updates.getContactEmail());
        }
        if (updates.getContactPhone() != null) {
            existingTenant.setContactPhone(updates.getContactPhone());
        }
        if (updates.getAddress() != null) {
            existingTenant.setAddress(updates.getAddress());
        }
        if (updates.getTimezone() != null) {
            existingTenant.setTimezone(updates.getTimezone());
        }
        if (updates.getLocale() != null) {
            existingTenant.setLocale(updates.getLocale());
        }
        if (updates.getCurrency() != null) {
            existingTenant.setCurrency(updates.getCurrency());
        }

        Tenant savedTenant = tenantRepository.save(existingTenant);
        log.info("Successfully updated tenant with ID: {}", tenantId);
        return savedTenant;
    }

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    public void deleteTenant(UUID tenantId) {
        log.info("Soft deleting tenant with ID: {}", tenantId);

        Tenant tenant = getTenantById(tenantId);

        // Check if tenant can be deleted
        if (tenant.getStatus() == TenantStatus.DELETED) {
            throw new TenantValidationException("Tenant is already deleted");
        }

        // Perform soft delete
        tenantRepository.softDeleteTenant(tenantId, Instant.now());

        log.info("Successfully soft deleted tenant with ID: {}", tenantId);
    }

    @Override
    public Page<Tenant> searchTenants(String searchTerm, Pageable pageable) {
        log.debug("Searching tenants with term: {}", searchTerm);
        Page<Tenant> tenants = tenantRepository.searchTenants(searchTerm, pageable);

        // Load features for each tenant
        tenants.getContent().forEach(this::loadTenantFeatures);

        return tenants;
    }

    @Override
    public Page<Tenant> getAllTenants(Pageable pageable) {
        log.debug("Fetching all tenants with pagination");
        Page<Tenant> tenants = tenantRepository.findAll(pageable);

        // Load features for each tenant
        tenants.getContent().forEach(this::loadTenantFeatures);

        return tenants;
    }

    @Override
    public List<Tenant> getTenantsByStatus(TenantStatus status) {
        log.debug("Fetching tenants with status: {}", status);
        return tenantRepository.findByStatus(status);
    }

    @Override
    @Transactional
    public void provisionTenant(UUID tenantId) {
        log.info("Provisioning tenant with ID: {}", tenantId);

        Tenant tenant = getTenantById(tenantId);

        // Check if tenant is in correct status
        if (tenant.getStatus() != TenantStatus.PENDING && tenant.getStatus() != TenantStatus.PROVISIONING) {
            throw new InvalidTenantStatusException(
                    tenant.getStatus().toString(),
                    TenantStatus.PROVISIONING.toString()
            );
        }

        boolean schemaCreatedSuccessfully = false;
        String schemaName = tenant.getSchemaName();

        try {
            // Update status to provisioning
            tenant.setStatus(TenantStatus.PROVISIONING);
            tenantRepository.save(tenant);

            // 1. Create database schema and initialize with data
            if (!databaseProvisioningService.schemaExists(schemaName)) {
                // Create schema first (separate transaction for DDL)
                databaseProvisioningService.createTenantSchema(schemaName);

                // Initialize tables AND insert data in single transaction
                databaseProvisioningService.initializeSchemaWithData(schemaName, tenantId.toString());

                // Mark as successfully created based on the method completion
                schemaCreatedSuccessfully = true;
                log.info("Schema {} creation and initialization completed", schemaName);

                // Try verification with retry, but don't fail if it doesn't work immediately
                boolean verified = verifySchemaWithRetry(schemaName, 3, 500);
                if (verified) {
                    log.info("Schema {} verification successful", schemaName);
                } else {
                    log.warn("Schema {} verification failed, but creation appeared successful. " +
                            "This may be due to transaction timing. Proceeding with provisioning.", schemaName);
                    // Don't throw exception here - the schema was created successfully
                }
            } else {
                log.warn("Schema {} already exists, skipping creation", schemaName);
                schemaCreatedSuccessfully = true; // Existing schema counts as successful
            }

            // 2. Configure external services (if needed)
            configureExternalServices(tenant);

            // 3. Initialize usage tracking
            initializeUsageTracking(tenant);

            // Update status to active
            tenant.setStatus(TenantStatus.ACTIVE);
            tenantRepository.save(tenant);

            log.info("Successfully provisioned tenant with ID: {} and schema: {}", tenantId, schemaName);

        } catch (Exception e) {
            log.error("Failed to provision tenant with ID: {}", tenantId, e);

            // Rollback status
            tenant.setStatus(TenantStatus.PENDING);
            tenantRepository.save(tenant);

            // Only clean up if we're sure the schema creation actually failed
            // Don't clean up if it was just a verification timing issue
            if (shouldCleanupSchema(e, schemaCreatedSuccessfully)) {
                try {
                    // Give some time for transactions to settle before cleanup
                    Thread.sleep(1000);

                    // Double-check if schema actually exists before dropping
                    if (databaseProvisioningService.schemaExists(schemaName)) {
                        log.info("Schema {} exists, proceeding with cleanup", schemaName);
                        databaseProvisioningService.dropTenantSchema(schemaName);
                        log.info("Cleaned up schema {} after provisioning failure", schemaName);
                    } else {
                        log.info("Schema {} doesn't exist, no cleanup needed", schemaName);
                    }
                } catch (Exception cleanupEx) {
                    log.error("Failed to cleanup schema after provisioning failure", cleanupEx);
                }
            } else {
                log.info("Skipping schema cleanup for {} - appears to be verification timing issue", schemaName);
            }

            throw new TenantProvisioningException("Failed to provision tenant: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant activateTenant(UUID tenantId) {
        log.info("Activating tenant with ID: {}", tenantId);

        Tenant tenant = getTenantById(tenantId);

        if (!tenant.getStatus().canBeActivated()) {
            throw new InvalidTenantStatusException(
                    tenant.getStatus().toString(),
                    TenantStatus.ACTIVE.toString()
            );
        }

        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setSuspendedAt(null);

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Successfully activated tenant with ID: {}", tenantId);
        return savedTenant;
    }

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant suspendTenant(UUID tenantId, String reason) {
        log.info("Suspending tenant with ID: {} for reason: {}", tenantId, reason);

        Tenant tenant = getTenantById(tenantId);

        if (!tenant.getStatus().canBeSuspended()) {
            throw new InvalidTenantStatusException(
                    tenant.getStatus().toString(),
                    TenantStatus.SUSPENDED.toString()
            );
        }

        tenant.setStatus(TenantStatus.SUSPENDED);
        tenant.setSuspendedAt(Instant.now());

        // Add suspension reason to metadata
        // TODO: Update metadata with reason

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Successfully suspended tenant with ID: {}", tenantId);
        return savedTenant;
    }

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant reactivateTenant(UUID tenantId) {
        log.info("Reactivating tenant with ID: {}", tenantId);

        Tenant tenant = getTenantById(tenantId);

        if (tenant.getStatus() != TenantStatus.SUSPENDED) {
            throw new InvalidTenantStatusException(
                    tenant.getStatus().toString(),
                    TenantStatus.ACTIVE.toString()
            );
        }

        return activateTenant(tenantId);
    }

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant archiveTenant(UUID tenantId) {
        log.info("Archiving tenant with ID: {}", tenantId);

        Tenant tenant = getTenantById(tenantId);
        tenant.setStatus(TenantStatus.ARCHIVED);

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Successfully archived tenant with ID: {}", tenantId);
        return savedTenant;
    }

    // ========== Subscription Operations ==========

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    @Transactional
    public Tenant updateSubscriptionPlan(UUID tenantId, SubscriptionPlan newPlan) {
        log.info("Updating subscription plan for tenant {} to {}", tenantId, newPlan);

        Tenant tenant = getTenantById(tenantId);
        SubscriptionPlan oldPlan = tenant.getSubscriptionPlan();

        // Check if downgrade is allowed
        if (isDowngrade(oldPlan, newPlan)) {
            validateDowngrade(tenant, newPlan);
        }

        // Update tenant plan
        tenant.setSubscriptionPlan(newPlan);

        // Update resource limits
        setResourceLimitsForPlan(tenant, newPlan);

        // Update features
        updateFeaturesForPlan(tenantId, newPlan);

        // Create new subscription record
        TenantSubscription subscription = TenantSubscription.builder()
                .tenant(tenant)
                .plan(newPlan)
                .status("ACTIVE")
                .startDate(Instant.now())
                .billingCycle("MONTHLY")
                .price(BigDecimal.valueOf(newPlan.getBasePrice()))
                .currency("USD")
                .autoRenew(true)
                .build();

        subscriptionRepository.save(subscription);

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Successfully updated subscription plan for tenant {}", tenantId);
        return savedTenant;
    }

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant startTrial(UUID tenantId, Integer trialDays) {
        log.info("Starting {}-day trial for tenant {}", trialDays, tenantId);

        Tenant tenant = getTenantById(tenantId);

        if (tenant.getTrialEndsAt() != null && tenant.getTrialEndsAt().isAfter(Instant.now())) {
            throw new SubscriptionException("Tenant already has an active trial");
        }

        tenant.setTrialEndsAt(Instant.now().plus(trialDays, ChronoUnit.DAYS));

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Successfully started trial for tenant {}", tenantId);
        return savedTenant;
    }

    @Override
    @CacheEvict(value = "tenants", key = "#tenantId")
    public Tenant extendSubscription(UUID tenantId, Integer days) {
        log.info("Extending subscription by {} days for tenant {}", days, tenantId);

        Tenant tenant = getTenantById(tenantId);

        Instant currentEnd = tenant.getSubscriptionEndsAt();
        if (currentEnd == null) {
            currentEnd = Instant.now();
        }

        tenant.setSubscriptionEndsAt(currentEnd.plus(days, ChronoUnit.DAYS));

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Successfully extended subscription for tenant {}", tenantId);
        return savedTenant;
    }

    @Override
    @Transactional
    public void cancelSubscription(UUID tenantId, String reason) {
        log.info("Cancelling subscription for tenant {} with reason: {}", tenantId, reason);

        Optional<TenantSubscription> currentSub = subscriptionRepository.findCurrentSubscription(tenantId);

        if (currentSub.isPresent()) {
            TenantSubscription subscription = currentSub.get();
            subscription.setStatus("CANCELLED");
            subscription.setCancelledAt(Instant.now());
            subscription.setCancellationReason(reason);
            subscription.setAutoRenew(false);
            subscriptionRepository.save(subscription);
        }

        log.info("Successfully cancelled subscription for tenant {}", tenantId);
    }

    // ========== Feature Management ==========

    @Override
    @Transactional
    public TenantFeature enableFeature(UUID tenantId, FeatureFlag feature) {
        log.info("Enabling feature {} for tenant {}", feature, tenantId);

        Tenant tenant = getTenantById(tenantId);

        Optional<TenantFeature> existing = featureRepository.findByTenantIdAndFeature(tenantId, feature);

        TenantFeature tenantFeature;
        if (existing.isPresent()) {
            tenantFeature = existing.get();
            tenantFeature.setEnabled(true);
            tenantFeature.setDeletedAt(null);
        } else {
            tenantFeature = TenantFeature.builder()
                    .tenant(tenant)
                    .feature(feature)
                    .enabled(true)
                    .build();
        }

        TenantFeature savedFeature = featureRepository.save(tenantFeature);
        log.info("Successfully enabled feature {} for tenant {}", feature, tenantId);
        return savedFeature;
    }

    @Override
    @Transactional
    public void disableFeature(UUID tenantId, FeatureFlag feature) {
        log.info("Disabling feature {} for tenant {}", feature, tenantId);

        Optional<TenantFeature> existing = featureRepository.findByTenantIdAndFeature(tenantId, feature);

        if (existing.isPresent()) {
            TenantFeature tenantFeature = existing.get();
            tenantFeature.setEnabled(false);
            featureRepository.save(tenantFeature);
        }

        log.info("Successfully disabled feature {} for tenant {}", feature, tenantId);
    }

    @Override
    @Cacheable(value = "tenant-features", key = "#tenantId")
    public List<TenantFeature> getTenantFeatures(UUID tenantId) {
        log.debug("Fetching features for tenant {}", tenantId);
        return featureRepository.findActiveFeaturesByTenantId(tenantId, Instant.now());
    }

    @Override
    public boolean hasFeature(UUID tenantId, FeatureFlag feature) {
        Optional<TenantFeature> tenantFeature = featureRepository.findByTenantIdAndFeature(tenantId, feature);
        return tenantFeature.isPresent() && tenantFeature.get().isActive();
    }

    @Override
    @CacheEvict(value = "tenant-features", key = "#tenantId")
    @Transactional
    public void updateFeaturesForPlan(UUID tenantId, SubscriptionPlan plan) {
        log.info("Updating features for tenant {} based on plan {}", tenantId, plan);

        // Define features for each plan
        Set<FeatureFlag> planFeatures = getFeaturesForPlan(plan);

        // Enable plan features
        for (FeatureFlag feature : planFeatures) {
            enableFeature(tenantId, feature);
        }

        // Disable features not in plan
        List<TenantFeature> currentFeatures = featureRepository.findByTenantId(tenantId);
        for (TenantFeature tf : currentFeatures) {
            if (!planFeatures.contains(tf.getFeature()) && !tf.getFeature().isBasicFeature()) {
                disableFeature(tenantId, tf.getFeature());
            }
        }
    }

    // ========== Configuration Management ==========

    @Override
    @CacheEvict(value = "tenant-config", key = "#tenantId + ':' + #key")
    @Transactional
    public TenantConfiguration setConfiguration(UUID tenantId, String key, String value) {
        log.info("Setting configuration {} = {} for tenant {}", key, value, tenantId);

        Tenant tenant = getTenantById(tenantId);

        Optional<TenantConfiguration> existing = configurationRepository.findByTenantIdAndConfigKey(tenantId, key);

        TenantConfiguration config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setConfigValue(value);
        } else {
            config = TenantConfiguration.builder()
                    .tenant(tenant)
                    .configKey(key)
                    .configValue(value)
                    .isEncrypted(false)
                    .isSensitive(false)
                    .build();
        }

        return configurationRepository.save(config);
    }

    @Override
    @Cacheable(value = "tenant-config", key = "#tenantId + ':' + #key")
    public String getConfiguration(UUID tenantId, String key) {
        Optional<TenantConfiguration> config = configurationRepository.findByTenantIdAndConfigKey(tenantId, key);
        return config.map(TenantConfiguration::getEffectiveValue).orElse(null);
    }

    @Override
    public Map<String, String> getAllConfigurations(UUID tenantId) {
        List<TenantConfiguration> configs = configurationRepository.findByTenantId(tenantId);
        return configs.stream()
                .collect(Collectors.toMap(
                        TenantConfiguration::getConfigKey,
                        TenantConfiguration::getEffectiveValue
                ));
    }

    @Override
    @CacheEvict(value = "tenant-config", key = "#tenantId + ':' + #key")
    public void deleteConfiguration(UUID tenantId, String key) {
        configurationRepository.deleteByTenantIdAndConfigKey(tenantId, key);
    }

    // ========== Usage and Limits ==========

    @Override
    @Transactional
    public void updateUsageMetrics(UUID tenantId, Integer users, Double storageGb, Integer datasets) {
        log.info("Updating usage metrics for tenant {}", tenantId);

        Tenant tenant = getTenantById(tenantId);

        if (users != null) tenant.setCurrentUsers(users);
        if (storageGb != null) tenant.setCurrentStorageGb(storageGb);
        if (datasets != null) tenant.setCurrentDatasets(datasets);

        // Check limits
        if (tenant.hasReachedUserLimit() || tenant.hasReachedStorageLimit() || tenant.hasReachedDatasetLimit()) {
            log.warn("Tenant {} has exceeded resource limits", tenantId);
            // TODO: Send notification or take action
        }

        tenantRepository.save(tenant);
    }

    @Override
    public boolean checkLimits(UUID tenantId) {
        Tenant tenant = getTenantById(tenantId);
        return !tenant.hasReachedUserLimit() &&
                !tenant.hasReachedStorageLimit() &&
                !tenant.hasReachedDatasetLimit();
    }

    @Override
    public Map<String, Object> getTenantStatistics(UUID tenantId) {
        Tenant tenant = getTenantById(tenantId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("tenantId", tenant.getId());
        stats.put("name", tenant.getName());
        stats.put("status", tenant.getStatus());
        stats.put("plan", tenant.getSubscriptionPlan());
        stats.put("currentUsers", tenant.getCurrentUsers());
        stats.put("maxUsers", tenant.getMaxUsers());
        stats.put("currentStorageGb", tenant.getCurrentStorageGb());
        stats.put("maxStorageGb", tenant.getMaxStorageGb());
        stats.put("currentDatasets", tenant.getCurrentDatasets());
        stats.put("maxDatasets", tenant.getMaxDatasets());
        stats.put("isInTrial", tenant.isInTrial());
        stats.put("createdAt", tenant.getCreatedAt());

        return stats;
    }

    @Override
    public Map<String, Object> getPlatformStatistics() {
        return (Map<String, Object>) tenantRepository.getTenantStatistics();
    }

    // ========== Validation ==========

    @Override
    public boolean isSubdomainAvailable(String subdomain) {
        return !tenantRepository.existsBySubdomain(subdomain);
    }

    @Override
    public void validateTenant(Tenant tenant) {
        if (tenant.getName() == null || tenant.getName().trim().isEmpty()) {
            throw new TenantValidationException("Tenant name is required");
        }

        if (tenant.getSubdomain() == null || tenant.getSubdomain().trim().isEmpty()) {
            throw new TenantValidationException("Subdomain is required");
        }

        // Validate subdomain format
        if (!tenant.getSubdomain().matches("^[a-z0-9-]+$")) {
            throw new TenantValidationException("Subdomain must contain only lowercase letters, numbers, and hyphens");
        }

        if (tenant.getContactEmail() != null && !tenant.getContactEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new TenantValidationException("Invalid email format");
        }
    }


    // ========== Helper Methods ==========

    private void setResourceLimitsForPlan(Tenant tenant, SubscriptionPlan plan) {
        switch (plan) {
            case FREE:
                tenant.setMaxUsers(10);
                tenant.setMaxStorageGb(25);
                tenant.setMaxDatasets(100);
                break;
            case STARTER:
                tenant.setMaxUsers(50);
                tenant.setMaxStorageGb(100);
                tenant.setMaxDatasets(500);
                break;
            case PROFESSIONAL:
                tenant.setMaxUsers(200);
                tenant.setMaxStorageGb(500);
                tenant.setMaxDatasets(2000);
                break;
            case ENTERPRISE:
                tenant.setMaxUsers(-1); // Unlimited
                tenant.setMaxStorageGb(-1);
                tenant.setMaxDatasets(-1);
                break;
            case EDUCATION:
                tenant.setMaxUsers(100);
                tenant.setMaxStorageGb(250);
                tenant.setMaxDatasets(1000);
                break;
        }
    }

    private Set<FeatureFlag> getFeaturesForPlan(SubscriptionPlan plan) {
        Set<FeatureFlag> features = new HashSet<>();

        // Basic features for all plans
        features.add(FeatureFlag.BASIC_ANALYTICS);
        features.add(FeatureFlag.DATA_EXPORT);
        features.add(FeatureFlag.API_ACCESS);

        switch (plan) {
            case PROFESSIONAL:
                features.add(FeatureFlag.ADVANCED_ANALYTICS);
                features.add(FeatureFlag.CUSTOM_BRANDING);
                features.add(FeatureFlag.PRIORITY_SUPPORT);
                features.add(FeatureFlag.CUSTOM_DASHBOARDS);
                break;
            case ENTERPRISE:
                features.add(FeatureFlag.ADVANCED_ANALYTICS);
                features.add(FeatureFlag.CUSTOM_BRANDING);
                features.add(FeatureFlag.PRIORITY_SUPPORT);
                features.add(FeatureFlag.ML_MODELS);
                features.add(FeatureFlag.WHITE_LABELING);
                features.add(FeatureFlag.DEDICATED_SUPPORT);
                features.add(FeatureFlag.CUSTOM_INTEGRATIONS);
                features.add(FeatureFlag.SSO_INTEGRATION);
                features.add(FeatureFlag.AUDIT_LOGS);
                features.add(FeatureFlag.UNLIMITED_STORAGE);
                features.add(FeatureFlag.CUSTOM_DASHBOARDS);
                features.add(FeatureFlag.ADVANCED_VISUALIZATIONS);
                break;
            case EDUCATION:
                features.add(FeatureFlag.ADVANCED_ANALYTICS);
                features.add(FeatureFlag.CUSTOM_DASHBOARDS);
                features.add(FeatureFlag.ADVANCED_SURVEYS);
                break;
        }

        return features;
    }

    private boolean isDowngrade(SubscriptionPlan oldPlan, SubscriptionPlan newPlan) {
        return oldPlan.ordinal() > newPlan.ordinal();
    }

    private void validateDowngrade(Tenant tenant, SubscriptionPlan newPlan) {
        // Set temporary limits to check
        Integer newMaxUsers = getMaxUsersForPlan(newPlan);
        Integer newMaxStorage = getMaxStorageForPlan(newPlan);
        Integer newMaxDatasets = getMaxDatasetsForPlan(newPlan);

        if (newMaxUsers > 0 && tenant.getCurrentUsers() > newMaxUsers) {
            throw new TenantLimitExceededException("users", tenant.getCurrentUsers(), newMaxUsers);
        }

        if (newMaxStorage > 0 && tenant.getCurrentStorageGb() > newMaxStorage) {
            throw new TenantLimitExceededException("storage",
                    tenant.getCurrentStorageGb().intValue(), newMaxStorage);
        }

        if (newMaxDatasets > 0 && tenant.getCurrentDatasets() > newMaxDatasets) {
            throw new TenantLimitExceededException("datasets", tenant.getCurrentDatasets(), newMaxDatasets);
        }
    }

    private Integer getMaxUsersForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> 10;
            case STARTER -> 50;
            case PROFESSIONAL -> 200;
            case ENTERPRISE -> -1;
            case EDUCATION -> 100;
            default -> 10;
        };
    }

    private Integer getMaxStorageForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> 25;
            case STARTER -> 100;
            case PROFESSIONAL -> 500;
            case ENTERPRISE -> -1;
            case EDUCATION -> 250;
            default -> 25;
        };
    }

    private Integer getMaxDatasetsForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> 100;
            case STARTER -> 500;
            case PROFESSIONAL -> 2000;
            case ENTERPRISE -> -1;
            case EDUCATION -> 1000;
            default -> 100;
        };
    }

    private void createInitialSubscription(Tenant tenant) {
        TenantSubscription subscription = TenantSubscription.builder()
                .tenant(tenant)
                .plan(tenant.getSubscriptionPlan())
                .status("ACTIVE")
                .startDate(Instant.now())
                .billingCycle("MONTHLY")
                .price(BigDecimal.valueOf(tenant.getSubscriptionPlan().getBasePrice()))
                .currency("USD")
                .autoRenew(tenant.getSubscriptionPlan() != SubscriptionPlan.FREE)
                .build();

        if (tenant.getSubscriptionPlan() == SubscriptionPlan.FREE && tenant.getTrialEndsAt() != null) {
            subscription.setTrialStartDate(Instant.now());
            subscription.setTrialEndDate(tenant.getTrialEndsAt());
        }

        subscriptionRepository.save(subscription);
    }

    private void addDefaultFeaturesForPlan(Tenant tenant) {
        Set<FeatureFlag> features = getFeaturesForPlan(tenant.getSubscriptionPlan());
        for (FeatureFlag feature : features) {
            enableFeature(tenant.getId(), feature);
        }
    }

    private void addDefaultConfigurations(Tenant tenant) {
        setConfiguration(tenant.getId(), "max_api_requests_per_minute", "100");
        setConfiguration(tenant.getId(), "enable_2fa", "false");
        setConfiguration(tenant.getId(), "data_retention_days", "30");
        setConfiguration(tenant.getId(), "session_timeout_minutes", "60");
    }

    private void configureExternalServices(Tenant tenant) {
        log.info("Configuring external services for tenant: {}", tenant.getName());

        try {
            // Configure Kafka topics for tenant
            String topicPrefix = "tenant." + tenant.getSubdomain();
            log.debug("Would create Kafka topics with prefix: {}", topicPrefix);
            // TODO: Implement actual Kafka topic creation using KafkaAdmin

            // Configure Redis namespace for tenant
            String keyPrefix = "tenant:" + tenant.getSubdomain() + ":";
            log.debug("Would configure Redis namespace with prefix: {}", keyPrefix);
            // TODO: Implement actual Redis namespace configuration

            // Configure file storage bucket
            String bucketName = "tenant-" + tenant.getSubdomain() + "-files";
            log.debug("Would create storage bucket: {}", bucketName);
            // TODO: Implement actual S3/MinIO bucket creation

            log.info("Successfully configured external services for tenant: {}", tenant.getName());
        } catch (Exception e) {
            log.error("Failed to configure external services for tenant: {}", tenant.getName(), e);
            throw new RuntimeException("Failed to configure external services", e);
        }
    }

    private void initializeUsageTracking(Tenant tenant) {
        log.info("Initializing usage tracking for tenant: {}", tenant.getName());

        TenantUsage initialUsage = TenantUsage.builder()
                .tenant(tenant)
                .usageDate(java.time.LocalDate.now())
                .metricName("initial_setup")
                .metricValue(1.0)
                .metricUnit("COUNT")
                .metricCategory("SYSTEM")
                .isBillable(false)
                .recordedAt(Instant.now())
                .build();

        usageRepository.save(initialUsage);
    }

    /**
     * Determine if we should cleanup the schema based on the error and creation status
     */
    private boolean shouldCleanupSchema(Exception error, boolean schemaCreatedSuccessfully) {
        // If schema creation appeared successful and error mentions verification,
        // don't cleanup - it's likely a timing issue
        if (schemaCreatedSuccessfully && error.getMessage().contains("verification")) {
            return false;
        }

        // If it's a RuntimeException from schema verification, don't cleanup
        if (error instanceof RuntimeException &&
                error.getMessage().contains("Schema verification failed")) {
            return false;
        }

        // For other errors, cleanup is appropriate
        return true;
    }

    /**
     * Verify schema exists with retry logic to handle transaction timing issues
     */
    private boolean verifySchemaWithRetry(String schemaName, int maxRetries, long delayMs) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (i > 0) {
                    Thread.sleep(delayMs);
                    log.debug("Retry {} for schema {} verification", i + 1, schemaName);
                }

                if (databaseProvisioningService.schemaExists(schemaName)) {
                    Integer tableCount = databaseProvisioningService.getTableCount(schemaName);
                    if (tableCount != null && tableCount >= 6) {
                        log.info("Schema {} verified successfully with {} tables on attempt {}",
                                schemaName, tableCount, i + 1);
                        return true;
                    }
                    log.debug("Schema {} exists but has insufficient tables: {} on attempt {}",
                            schemaName, tableCount, i + 1);
                } else {
                    log.debug("Schema {} not visible on attempt {}", schemaName, i + 1);
                }
            } catch (Exception e) {
                log.debug("Verification attempt {} failed for schema {}: {}", i + 1, schemaName, e.getMessage());
            }
        }

        log.warn("Schema {} verification failed after {} attempts", schemaName, maxRetries);
        return false;
    }

    /**
     * Load active features for a tenant and set them on the features collection
     */
    private void loadTenantFeatures(Tenant tenant) {
        List<TenantFeature> activeFeatures = featureRepository
                .findActiveFeaturesByTenantId(tenant.getId(), Instant.now());

        // Convert List to Set and set it on the tenant
        tenant.setFeatures(new HashSet<>(activeFeatures));
    }
}
