package com.nnipa.tenant.repository;

import com.nnipa.tenant.entity.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID; /**
 * Repository for TenantSubscription entity
 */
@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID> {

    List<TenantSubscription> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.tenant.id = :tenantId " +
            "AND ts.status = 'ACTIVE' ORDER BY ts.createdAt DESC")
    Optional<TenantSubscription> findCurrentSubscription(@Param("tenantId") UUID tenantId);

    @Query("SELECT ts FROM TenantSubscription ts WHERE ts.endDate BETWEEN :start AND :end " +
            "AND ts.status = 'ACTIVE' AND ts.autoRenew = true")
    List<TenantSubscription> findSubscriptionsForRenewal(@Param("start") Instant start,
                                                         @Param("end") Instant end);
}
