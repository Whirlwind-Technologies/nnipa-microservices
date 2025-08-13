package com.nnipa.tenant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for tenant resource limits
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resource limits for the tenant")
public class ResourceLimitsResponse {

    @Schema(description = "Maximum number of users allowed", example = "200")
    private Integer maxUsers;

    @Schema(description = "Maximum storage in GB", example = "500")
    private Integer maxStorageGb;

    @Schema(description = "Maximum number of datasets", example = "2000")
    private Integer maxDatasets;

    @Schema(description = "Maximum API requests per minute", example = "1000")
    private Integer maxApiRequestsPerMinute;

    @Schema(description = "Maximum number of concurrent sessions", example = "50")
    private Integer maxConcurrentSessions;

    @Schema(description = "Whether unlimited users are allowed", example = "false")
    private Boolean unlimitedUsers;

    @Schema(description = "Whether unlimited storage is allowed", example = "false")
    private Boolean unlimitedStorage;

    @Schema(description = "Whether unlimited datasets are allowed", example = "false")
    private Boolean unlimitedDatasets;
}