package com.nnipa.tenant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for tenant usage statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current usage statistics for the tenant")
public class UsageStatisticsResponse {

    @Schema(description = "Current number of active users", example = "45")
    private Integer currentUsers;

    @Schema(description = "Current storage usage in GB", example = "125.5")
    private Double currentStorageGb;

    @Schema(description = "Current number of datasets", example = "350")
    private Integer currentDatasets;

    @Schema(description = "API calls made today", example = "15234")
    private Long apiCallsToday;

    @Schema(description = "API calls made this month", example = "450000")
    private Long apiCallsThisMonth;

    @Schema(description = "User utilization percentage", example = "22.5")
    private Double userUtilizationPercent;

    @Schema(description = "Storage utilization percentage", example = "25.1")
    private Double storageUtilizationPercent;

    @Schema(description = "Dataset utilization percentage", example = "17.5")
    private Double datasetUtilizationPercent;

    @Schema(description = "Whether user limit is reached", example = "false")
    private Boolean userLimitReached;

    @Schema(description = "Whether storage limit is reached", example = "false")
    private Boolean storageLimitReached;

    @Schema(description = "Whether dataset limit is reached", example = "false")
    private Boolean datasetLimitReached;
}