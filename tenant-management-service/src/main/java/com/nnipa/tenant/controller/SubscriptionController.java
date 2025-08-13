package com.nnipa.tenant.controller;

import com.nnipa.tenant.dto.request.UpdateSubscriptionRequest;
import com.nnipa.tenant.dto.response.ApiResponse;
import com.nnipa.tenant.dto.response.TenantResponse;
import com.nnipa.tenant.entity.Tenant;
import com.nnipa.tenant.mapper.TenantMapper;
import com.nnipa.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for subscription management operations
 */
@Slf4j
@RestController
@RequestMapping("/tenants/{tenantId}/subscription")
@RequiredArgsConstructor
@Tag(name = "Subscription Management", description = "APIs for managing tenant subscriptions")
public class SubscriptionController {

    private final TenantService tenantService;
    private final TenantMapper tenantMapper;

    /**
     * Update subscription plan
     */
    @PutMapping
    @Operation(summary = "Update subscription plan", description = "Changes the subscription plan for a tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Downgrade not allowed due to resource usage")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> updateSubscription(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {

        log.info("Updating subscription for tenant {} to plan {}", tenantId, request.getPlan());

        Tenant tenant = tenantService.updateSubscriptionPlan(tenantId, request.getPlan());
        TenantResponse response = tenantMapper.toResponse(tenant);

        log.info("Successfully updated subscription for tenant {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Subscription updated successfully"));
    }

    /**
     * Start trial for tenant
     */
    @PostMapping("/trial")
    @Operation(summary = "Start trial", description = "Starts a trial period for the tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trial started successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Trial already active or not available"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> startTrial(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Parameter(description = "Trial days") @RequestParam(defaultValue = "30") Integer trialDays) {

        log.info("Starting {}-day trial for tenant {}", trialDays, tenantId);

        Tenant tenant = tenantService.startTrial(tenantId, trialDays);
        TenantResponse response = tenantMapper.toResponse(tenant);

        log.info("Successfully started trial for tenant {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Trial started successfully"));
    }

    /**
     * Extend subscription
     */
    @PostMapping("/extend")
    @Operation(summary = "Extend subscription", description = "Extends the subscription period")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription extended successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> extendSubscription(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Parameter(description = "Extension days") @RequestParam Integer days) {

        log.info("Extending subscription by {} days for tenant {}", days, tenantId);

        Tenant tenant = tenantService.extendSubscription(tenantId, days);
        TenantResponse response = tenantMapper.toResponse(tenant);

        log.info("Successfully extended subscription for tenant {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Subscription extended successfully"));
    }

    /**
     * Cancel subscription
     */
    @PostMapping("/cancel")
    @Operation(summary = "Cancel subscription", description = "Cancels the tenant's subscription")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Subscription cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> cancelSubscription(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Parameter(description = "Cancellation reason") @RequestParam String reason) {

        log.info("Cancelling subscription for tenant {} with reason: {}", tenantId, reason);

        tenantService.cancelSubscription(tenantId, reason);

        Map<String, String> response = Map.of(
                "status", "cancelled",
                "message", "Subscription has been cancelled successfully"
        );

        log.info("Successfully cancelled subscription for tenant {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Subscription cancelled"));
    }
}