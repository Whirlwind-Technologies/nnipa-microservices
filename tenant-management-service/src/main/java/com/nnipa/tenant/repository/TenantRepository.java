package com.nnipa.tenant.repository;

import com.nnipa.tenant.entity.Tenant;
import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Tenant entity operations.
 * Provides standard CRUD operations and custom queries.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Find tenant by subdomain
     */
    Optional<Tenant> findBySubdomain(String subdomain);

    /**
     * Find tenant by schema name
     */
    Optional<Tenant> findBySchemaName(String schemaName);

    /**
     * Check if subdomain exists
     */
    boolean existsBySubdomain(String subdomain);

    /**
     * Check if schema name exists
     */
    boolean existsBySchemaName(String schemaName);

    /**
     * Find tenants by status
     */
    List<Tenant> findByStatus(TenantStatus status);

    /**
     * Find tenants by subscription plan
     */
    List<Tenant> findBySubscriptionPlan(SubscriptionPlan plan);

    /**
     * Find all active tenants (not deleted)
     */
    @Query("SELECT t FROM Tenant t WHERE t.deletedAt IS NULL AND t.status = :status")
    List<Tenant> findActiveTenantsWithStatus(@Param("status") TenantStatus status);

    /**
     * Find tenants with expiring trials
     */
    @Query("SELECT t FROM Tenant t WHERE t.trialEndsAt BETWEEN :now AND :futureDate " +
            "AND t.status = 'ACTIVE' AND t.deletedAt IS NULL")
    List<Tenant> findTenantsWithExpiringTrials(@Param("now") Instant now,
                                               @Param("futureDate") Instant futureDate);

    /**
     * Find tenants with expired subscriptions
     */
    @Query("SELECT t FROM Tenant t WHERE t.subscriptionEndsAt < :now " +
            "AND t.status = 'ACTIVE' AND t.deletedAt IS NULL")
    List<Tenant> findTenantsWithExpiredSubscriptions(@Param("now") Instant now);

    /**
     * Search tenants by name or subdomain
     */
    @Query("SELECT t FROM Tenant t WHERE " +
            "(LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.subdomain) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND t.deletedAt IS NULL")
    Page<Tenant> searchTenants(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find tenants by contact email
     */
    Optional<Tenant> findByContactEmail(String email);

    /**
     * Count tenants by status
     */
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = :status AND t.deletedAt IS NULL")
    Long countByStatusActive(@Param("status") TenantStatus status);

    /**
     * Update tenant usage metrics
     */
    @Modifying
    @Query("UPDATE Tenant t SET t.currentUsers = :users, t.currentStorageGb = :storage, " +
            "t.currentDatasets = :datasets WHERE t.id = :tenantId")
    void updateUsageMetrics(@Param("tenantId") UUID tenantId,
                            @Param("users") Integer users,
                            @Param("storage") Double storage,
                            @Param("datasets") Integer datasets);

    /**
     * Soft delete tenant
     */
    @Modifying
    @Query("UPDATE Tenant t SET t.deletedAt = :deletedAt, t.status = 'DELETED' WHERE t.id = :tenantId")
    void softDeleteTenant(@Param("tenantId") UUID tenantId, @Param("deletedAt") Instant deletedAt);

    /**
     * Find tenants exceeding storage limits
     */
    @Query("SELECT t FROM Tenant t WHERE t.currentStorageGb > t.maxStorageGb " +
            "AND t.maxStorageGb > 0 AND t.status = 'ACTIVE' AND t.deletedAt IS NULL")
    List<Tenant> findTenantsExceedingStorageLimits();

    /**
     * Get tenant statistics
     */
    @Query("SELECT new map(" +
            "COUNT(t) as totalTenants, " +
            "SUM(CASE WHEN t.status = 'ACTIVE' THEN 1 ELSE 0 END) as activeTenants, " +
            "SUM(t.currentUsers) as totalUsers, " +
            "SUM(t.currentStorageGb) as totalStorage) " +
            "FROM Tenant t WHERE t.deletedAt IS NULL")
    Object getTenantStatistics();
}