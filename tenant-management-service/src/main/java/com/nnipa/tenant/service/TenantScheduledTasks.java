package com.nnipa.tenant.service;

import com.nnipa.tenant.entity.Tenant;
import com.nnipa.tenant.enums.TenantStatus;
import com.nnipa.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled tasks for tenant management.
 * Handles automated processes like trial expiration, subscription renewal, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantScheduledTasks {

    private final TenantRepository tenantRepository;
    private final TenantService tenantService;

    /**
     * Check for expiring trials daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void checkExpiringTrials() {
        log.info("Starting scheduled task: Check expiring trials");

        Instant now = Instant.now();
        Instant threeDaysFromNow = now.plus(3, ChronoUnit.DAYS);

        List<Tenant> expiringTrials = tenantRepository.findTenantsWithExpiringTrials(now, threeDaysFromNow);

        for (Tenant tenant : expiringTrials) {
            log.info("Trial expiring soon for tenant: {} ({})", tenant.getName(), tenant.getId());
            // TODO: Send notification email
            // TODO: Create notification record
        }

        log.info("Completed checking expiring trials. Found {} tenants", expiringTrials.size());
    }

    /**
     * Check for expired subscriptions daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void checkExpiredSubscriptions() {
        log.info("Starting scheduled task: Check expired subscriptions");

        List<Tenant> expiredSubscriptions = tenantRepository.findTenantsWithExpiredSubscriptions(Instant.now());

        for (Tenant tenant : expiredSubscriptions) {
            log.info("Subscription expired for tenant: {} ({})", tenant.getName(), tenant.getId());

            try {
                // Suspend tenant if subscription expired
                tenantService.suspendTenant(tenant.getId(), "Subscription expired");
                // TODO: Send notification email
            } catch (Exception e) {
                log.error("Failed to suspend tenant {} after subscription expiration", tenant.getId(), e);
            }
        }

        log.info("Completed checking expired subscriptions. Found {} tenants", expiredSubscriptions.size());
    }

    /**
     * Check storage limits hourly
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void checkStorageLimits() {
        log.debug("Starting scheduled task: Check storage limits");

        List<Tenant> tenantsExceedingLimits = tenantRepository.findTenantsExceedingStorageLimits();

        for (Tenant tenant : tenantsExceedingLimits) {
            log.warn("Tenant {} has exceeded storage limit: {}/{} GB",
                    tenant.getName(),
                    tenant.getCurrentStorageGb(),
                    tenant.getMaxStorageGb());
            // TODO: Send warning notification
            // TODO: Consider throttling or blocking new uploads
        }

        if (!tenantsExceedingLimits.isEmpty()) {
            log.info("Found {} tenants exceeding storage limits", tenantsExceedingLimits.size());
        }
    }

    /**
     * Clean up deleted tenants older than 90 days - runs weekly
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    @Transactional
    public void cleanupDeletedTenants() {
        log.info("Starting scheduled task: Cleanup deleted tenants");

        Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);

        List<Tenant> deletedTenants = tenantRepository.findByStatus(TenantStatus.DELETED);
        int cleanedCount = 0;

        for (Tenant tenant : deletedTenants) {
            if (tenant.getDeletedAt() != null && tenant.getDeletedAt().isBefore(ninetyDaysAgo)) {
                log.info("Permanently deleting tenant: {} ({})", tenant.getName(), tenant.getId());
                // TODO: Archive data to cold storage
                // TODO: Remove from database
                cleanedCount++;
            }
        }

        log.info("Completed cleanup of deleted tenants. Cleaned {} tenants", cleanedCount);
    }

    /**
     * Update tenant statistics daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateTenantStatistics() {
        log.info("Starting scheduled task: Update tenant statistics");

        try {
            // Get all active tenants
            List<Tenant> activeTenants = tenantRepository.findActiveTenantsWithStatus(TenantStatus.ACTIVE);

            for (Tenant tenant : activeTenants) {
                // TODO: Calculate actual usage from various services
                // For now, this is a placeholder
                log.debug("Updating statistics for tenant: {}", tenant.getName());
            }

            log.info("Completed updating tenant statistics for {} tenants", activeTenants.size());

        } catch (Exception e) {
            log.error("Failed to update tenant statistics", e);
        }
    }

    /**
     * Health check - runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void performHealthCheck() {
        log.debug("Tenant management service health check - OK");

        // Count active tenants for monitoring
        Long activeCount = tenantRepository.countByStatusActive(TenantStatus.ACTIVE);
        log.debug("Active tenants count: {}", activeCount);
    }
}