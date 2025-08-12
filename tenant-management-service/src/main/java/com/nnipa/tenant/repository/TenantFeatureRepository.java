package com.nnipa.tenant.repository;

import com.nnipa.tenant.entity.*;
import com.nnipa.tenant.enums.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TenantFeature entity
 */
@Repository
public interface TenantFeatureRepository extends JpaRepository<TenantFeature, UUID> {

    List<TenantFeature> findByTenantId(UUID tenantId);

    Optional<TenantFeature> findByTenantIdAndFeature(UUID tenantId, FeatureFlag feature);

    @Query("SELECT tf FROM TenantFeature tf WHERE tf.tenant.id = :tenantId " +
            "AND tf.enabled = true AND tf.deletedAt IS NULL " +
            "AND (tf.expiresAt IS NULL OR tf.expiresAt > :now)")
    List<TenantFeature> findActiveFeaturesByTenantId(@Param("tenantId") UUID tenantId,
                                                     @Param("now") Instant now);

    void deleteByTenantIdAndFeature(UUID tenantId, FeatureFlag feature);
}

