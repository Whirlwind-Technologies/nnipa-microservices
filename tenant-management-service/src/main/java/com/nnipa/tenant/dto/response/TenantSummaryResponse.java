package com.nnipa.tenant.dto.response;

import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Simplified response DTO for tenant listing operations.
 * Does not include lazy-loaded collections to avoid performance issues.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Simplified tenant information for list operations")
public class TenantSummaryResponse {

    @Schema(description = "Unique tenant identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Tenant organization name", example = "Acme Corporation")
    private String name;

    @Schema(description = "Display name for UI", example = "Acme Corp")
    private String displayName;

    @Schema(description = "Unique subdomain", example = "acme")
    private String subdomain;

    @Schema(description = "Current tenant status")
    private TenantStatus status;

    @Schema(description = "Current subscription plan")
    private SubscriptionPlan subscriptionPlan;

    @Schema(description = "Contact email")
    private String contactEmail;

    @Schema(description = "Whether tenant is active")
    private Boolean isActive;

    @Schema(description = "Whether tenant is in trial period")
    private Boolean isInTrial;

    @Schema(description = "Current number of users")
    private Integer currentUsers;

    @Schema(description = "Maximum allowed users")
    private Integer maxUsers;

    @Schema(description = "Current storage usage in GB")
    private Double currentStorageGb;

    @Schema(description = "Maximum storage in GB")
    private Integer maxStorageGb;

    @Schema(description = "Trial end date")
    private Instant trialEndsAt;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;
}