package com.nnipa.tenant.mapper;

import com.nnipa.tenant.dto.request.CreateTenantRequest;
import com.nnipa.tenant.dto.request.TenantAddressRequest;
import com.nnipa.tenant.dto.request.UpdateTenantRequest;
import com.nnipa.tenant.dto.response.*;
import com.nnipa.tenant.entity.Tenant;
import com.nnipa.tenant.entity.TenantAddress;
import com.nnipa.tenant.entity.TenantFeature;
import com.nnipa.tenant.enums.FeatureFlag;
import org.mapstruct.*;
import org.hibernate.Hibernate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between DTOs and entities
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TenantMapper {

    /**
     * Convert CreateTenantRequest to Tenant entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "features", ignore = true)
    @Mapping(target = "configurations", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    @Mapping(target = "usageRecords", ignore = true)
    @Mapping(target = "trialEndsAt", ignore = true)
    @Mapping(target = "subscriptionEndsAt", ignore = true)
    @Mapping(target = "suspendedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "maxUsers", ignore = true)
    @Mapping(target = "maxStorageGb", ignore = true)
    @Mapping(target = "maxDatasets", ignore = true)
    @Mapping(target = "currentUsers", constant = "0")
    @Mapping(target = "currentStorageGb", constant = "0.0")
    @Mapping(target = "currentDatasets", constant = "0")
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "tags", ignore = true)
    Tenant toEntity(CreateTenantRequest request);

    /**
     * Convert UpdateTenantRequest to update existing Tenant entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subdomain", ignore = true)
    @Mapping(target = "schemaName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subscriptionPlan", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "features", ignore = true)
    @Mapping(target = "configurations", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    @Mapping(target = "usageRecords", ignore = true)
    void updateEntity(@MappingTarget Tenant tenant, UpdateTenantRequest request);

    /**
     * Convert Tenant entity to TenantResponse DTO
     */
    @Mapping(target = "resourceLimits", expression = "java(mapResourceLimits(tenant))")
    @Mapping(target = "usageStatistics", expression = "java(mapUsageStatistics(tenant))")
    @Mapping(target = "isInTrial", expression = "java(tenant.isInTrial())")
    @Mapping(target = "isActive", expression = "java(tenant.isActive())")
    @Mapping(target = "enabledFeatures", expression = "java(mapEnabledFeatures(tenant.getFeatures()))")
    TenantResponse toResponse(Tenant tenant);

    /**
     * Convert Tenant entity to TenantSummaryResponse DTO (for list operations)
     */
    @Mapping(target = "isActive", expression = "java(tenant.isActive())")
    @Mapping(target = "isInTrial", expression = "java(tenant.isInTrial())")
    TenantSummaryResponse toSummaryResponse(Tenant tenant);

    /**
     * Convert TenantAddressRequest to TenantAddress entity
     */
    TenantAddress toAddressEntity(TenantAddressRequest request);

    /**
     * Convert TenantAddress entity to TenantAddressResponse
     */
    @Mapping(target = "formattedAddress", expression = "java(address != null ? address.getFormattedAddress() : null)")
    TenantAddressResponse toAddressResponse(TenantAddress address);

    /**
     * Map resource limits from Tenant entity
     */
    default ResourceLimitsResponse mapResourceLimits(Tenant tenant) {
        if (tenant == null) return null;

        return ResourceLimitsResponse.builder()
                .maxUsers(tenant.getMaxUsers())
                .maxStorageGb(tenant.getMaxStorageGb())
                .maxDatasets(tenant.getMaxDatasets())
                .maxApiRequestsPerMinute(1000) // Could be from configuration
                .maxConcurrentSessions(50) // Could be from configuration
                .unlimitedUsers(tenant.getMaxUsers() != null && tenant.getMaxUsers() < 0)
                .unlimitedStorage(tenant.getMaxStorageGb() != null && tenant.getMaxStorageGb() < 0)
                .unlimitedDatasets(tenant.getMaxDatasets() != null && tenant.getMaxDatasets() < 0)
                .build();
    }

    /**
     * Map usage statistics from Tenant entity
     */
    default UsageStatisticsResponse mapUsageStatistics(Tenant tenant) {
        if (tenant == null) return null;

        Integer maxUsers = tenant.getMaxUsers() != null && tenant.getMaxUsers() > 0 ? tenant.getMaxUsers() : 1;
        Integer maxStorage = tenant.getMaxStorageGb() != null && tenant.getMaxStorageGb() > 0 ? tenant.getMaxStorageGb() : 1;
        Integer maxDatasets = tenant.getMaxDatasets() != null && tenant.getMaxDatasets() > 0 ? tenant.getMaxDatasets() : 1;

        return UsageStatisticsResponse.builder()
                .currentUsers(tenant.getCurrentUsers())
                .currentStorageGb(tenant.getCurrentStorageGb())
                .currentDatasets(tenant.getCurrentDatasets())
                .userUtilizationPercent((tenant.getCurrentUsers() * 100.0) / maxUsers)
                .storageUtilizationPercent((tenant.getCurrentStorageGb() * 100.0) / maxStorage)
                .datasetUtilizationPercent((tenant.getCurrentDatasets() * 100.0) / maxDatasets)
                .userLimitReached(tenant.hasReachedUserLimit())
                .storageLimitReached(tenant.hasReachedStorageLimit())
                .datasetLimitReached(tenant.hasReachedDatasetLimit())
                .build();
    }

    /**
     * Map enabled features from TenantFeature entities
     * Updated to return List<String> and handle features loaded by service
     */
    default List<String> mapEnabledFeatures(Set<TenantFeature> features) {
        if (features == null) {
            return Collections.emptyList();
        }

        try {
            // Check if the collection is initialized
            if (!Hibernate.isInitialized(features)) {
                // Return empty list if not initialized to avoid LazyInitializationException
                return Collections.emptyList();
            }

            return features.stream()
                    .filter(tf -> {
                        // Additional safety checks since features are now explicitly loaded
                        if (tf == null || tf.getFeature() == null) return false;

                        // Check if feature is enabled and not deleted
                        if (tf.getEnabled() == null || !tf.getEnabled() || tf.getDeletedAt() != null) {
                            return false;
                        }

                        // Check if feature is not expired
                        if (tf.getExpiresAt() != null && tf.getExpiresAt().isBefore(Instant.now())) {
                            return false;
                        }

                        return true;
                    })
                    .map(TenantFeature::getFeature)
                    .map(FeatureFlag::name)
                    .sorted() // Sort alphabetically for consistent output
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // If any exception occurs (including LazyInitializationException), return empty list
            return Collections.emptyList();
        }
    }
}