package com.nnipa.tenant.repository;

import com.nnipa.tenant.entity.TenantUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID; /**
 * Repository for TenantUsage entity
 */
@Repository
public interface TenantUsageRepository extends JpaRepository<TenantUsage, UUID> {

    List<TenantUsage> findByTenantIdAndUsageDate(UUID tenantId, LocalDate usageDate);

    @Query("SELECT tu FROM TenantUsage tu WHERE tu.tenant.id = :tenantId " +
            "AND tu.usageDate BETWEEN :startDate AND :endDate")
    List<TenantUsage> findByTenantIdAndDateRange(@Param("tenantId") UUID tenantId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(tu.metricValue) FROM TenantUsage tu WHERE tu.tenant.id = :tenantId " +
            "AND tu.metricName = :metricName AND tu.usageDate = :date")
    Double sumMetricForDate(@Param("tenantId") UUID tenantId,
                            @Param("metricName") String metricName,
                            @Param("date") LocalDate date);

    @Query("SELECT tu FROM TenantUsage tu WHERE tu.tenant.id = :tenantId " +
            "AND tu.metricName = :metricName ORDER BY tu.usageDate DESC")
    List<TenantUsage> findLatestUsageByMetric(@Param("tenantId") UUID tenantId,
                                              @Param("metricName") String metricName);
}
