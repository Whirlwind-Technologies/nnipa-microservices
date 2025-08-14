package com.nnipa.tenant.dto.response;

import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for tenant information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tenant information response")
public class TenantResponse {

    @Schema(description = "Unique tenant identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Tenant organization name", example = "Acme Corporation")
    private String name;

    @Schema(description = "Display name for UI", example = "Acme Corp")
    private String displayName;

    @Schema(description = "Unique subdomain", example = "acme")
    private String subdomain;

    @Schema(description = "Database schema name", example = "tenant_acme")
    private String schemaName;

    @Schema(description = "Current tenant status")
    private TenantStatus status;

    @Schema(description = "Current subscription plan")
    private SubscriptionPlan subscriptionPlan;

    @Schema(description = "Organization description")
    private String description;

    @Schema(description = "Logo URL")
    private String logoUrl;

    @Schema(description = "Website URL")
    private String website;

    @Schema(description = "Contact email")
    private String contactEmail;

    @Schema(description = "Contact phone")
    private String contactPhone;

    @Schema(description = "Address information")
    private TenantAddressResponse address;

    @Schema(description = "Timezone")
    private String timezone;

    @Schema(description = "Locale")
    private String locale;

    @Schema(description = "Currency code")
    private String currency;

    @Schema(description = "Resource limits")
    private ResourceLimitsResponse resourceLimits;

    @Schema(description = "Current usage statistics")
    private UsageStatisticsResponse usageStatistics;

    @Schema(description = "Trial end date")
    private Instant trialEndsAt;

    @Schema(description = "Subscription end date")
    private Instant subscriptionEndsAt;

    @Schema(description = "Whether tenant is in trial period")
    private Boolean isInTrial;

    @Schema(description = "Whether tenant is active")
    private Boolean isActive;

    @Schema(description = "Enabled features for this tenant")
    private List<String> enabledFeatures = new ArrayList<>();

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    @Schema(description = "Created by user")
    private String createdBy;

    @Schema(description = "Last updated by user")
    private String updatedBy;
}