package com.nnipa.tenant.repository;

import com.nnipa.tenant.entity.TenantConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID; /**
 * Repository for TenantConfiguration entity
 */
@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, UUID> {

    List<TenantConfiguration> findByTenantId(UUID tenantId);

    Optional<TenantConfiguration> findByTenantIdAndConfigKey(UUID tenantId, String configKey);

    List<TenantConfiguration> findByTenantIdAndCategory(UUID tenantId, String category);

    void deleteByTenantIdAndConfigKey(UUID tenantId, String configKey);
}
