package com.nnipa.tenant.controller;

import com.nnipa.tenant.dto.response.ApiResponse;
import com.nnipa.tenant.entity.TenantFeature;
import com.nnipa.tenant.enums.FeatureFlag;
import com.nnipa.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for feature flag management
 */
@Slf4j
@RestController
@RequestMapping("/tenants/{tenantId}/features")
@RequiredArgsConstructor
@Tag(name = "Feature Management", description = "APIs for managing tenant feature flags")
public class FeatureController {

    private final TenantService tenantService;

    /**
     * Get all features for a tenant
     */
    @GetMapping
    @Operation(summary = "Get tenant features", description = "Retrieves all feature flags for a tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Features retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTenantFeatures(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {

        log.debug("Fetching features for tenant {}", tenantId);

        List<TenantFeature> features = tenantService.getTenantFeatures(tenantId);

        List<Map<String, Object>> response = features.stream()
                .map(feature -> {
                    Map<String, Object> featureMap = new HashMap<>();
                    featureMap.put("feature", feature.getFeature().name());
                    featureMap.put("displayName", feature.getFeature().getDisplayName());
                    featureMap.put("description", feature.getFeature().getDescription());
                    featureMap.put("enabled", feature.getEnabled());
                    featureMap.put("active", feature.isActive());
                    featureMap.put("expiresAt", feature.getExpiresAt() != null ? feature.getExpiresAt().toString() : null);
                    return featureMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Enable a feature for a tenant
     */
    @PostMapping("/{feature}/enable")
    @Operation(summary = "Enable feature", description = "Enables a specific feature for the tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Feature enabled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid feature")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> enableFeature(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Parameter(description = "Feature flag") @PathVariable FeatureFlag feature) {

        log.info("Enabling feature {} for tenant {}", feature, tenantId);

        TenantFeature tenantFeature = tenantService.enableFeature(tenantId, feature);

        Map<String, Object> response = new HashMap<>();
        response.put("feature", tenantFeature.getFeature().name());
        response.put("enabled", tenantFeature.getEnabled());
        response.put("message", "Feature enabled successfully");

        log.info("Successfully enabled feature {} for tenant {}", feature, tenantId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Disable a feature for a tenant
     */
    @PostMapping("/{feature}/disable")
    @Operation(summary = "Disable feature", description = "Disables a specific feature for the tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Feature disabled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid feature")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> disableFeature(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Parameter(description = "Feature flag") @PathVariable FeatureFlag feature) {

        log.info("Disabling feature {} for tenant {}", feature, tenantId);

        tenantService.disableFeature(tenantId, feature);

        Map<String, String> response = Map.of(
                "feature", feature.name(),
                "status", "disabled",
                "message", "Feature disabled successfully"
        );

        log.info("Successfully disabled feature {} for tenant {}", feature, tenantId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Check if a feature is enabled for a tenant
     */
    @GetMapping("/{feature}/status")
    @Operation(summary = "Check feature status", description = "Checks if a specific feature is enabled for the tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkFeatureStatus(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Parameter(description = "Feature flag") @PathVariable FeatureFlag feature) {

        log.debug("Checking feature {} status for tenant {}", feature, tenantId);

        boolean hasFeature = tenantService.hasFeature(tenantId, feature);

        Map<String, Object> response = new HashMap<>();
        response.put("feature", feature.name());
        response.put("enabled", hasFeature);
        response.put("displayName", feature.getDisplayName());
        response.put("description", feature.getDescription());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}