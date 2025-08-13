package com.nnipa.tenant.controller;

import com.nnipa.tenant.dto.request.CreateTenantRequest;
import com.nnipa.tenant.dto.request.UpdateSubscriptionRequest;
import com.nnipa.tenant.dto.request.UpdateTenantRequest;
import com.nnipa.tenant.dto.response.ApiResponse;
import com.nnipa.tenant.dto.response.TenantResponse;
import com.nnipa.tenant.entity.Tenant;
import com.nnipa.tenant.entity.TenantFeature;
import com.nnipa.tenant.enums.FeatureFlag;
import com.nnipa.tenant.enums.TenantStatus;
import com.nnipa.tenant.mapper.TenantMapper;
import com.nnipa.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for tenant management operations
 */
@Slf4j
@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "APIs for managing tenants in the multi-tenant platform")
public class TenantController {

    private final TenantService tenantService;
    private final TenantMapper tenantMapper;

    /**
     * Register a new tenant
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new tenant", description = "Creates a new tenant with the provided information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tenant created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tenant already exists")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> registerTenant(
            @Valid @RequestBody CreateTenantRequest request) {

        log.info("Registering new tenant with subdomain: {}", request.getSubdomain());

        // Convert request to entity
        Tenant tenant = tenantMapper.toEntity(request);

        // Create tenant
        Tenant createdTenant = tenantService.createTenant(tenant);

        // Auto-provision if requested
        if (Boolean.TRUE.equals(request.getAutoProvision())) {
            tenantService.provisionTenant(createdTenant.getId());
        }

        // Convert to response
        TenantResponse response = tenantMapper.toResponse(createdTenant);

        log.info("Successfully registered tenant with ID: {}", createdTenant.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tenant registered successfully"));
    }

    /**
     * Get tenant by ID
     */
    @GetMapping("/{tenantId}")
    @Operation(summary = "Get tenant by ID", description = "Retrieves tenant information by tenant ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {

        log.debug("Fetching tenant with ID: {}", tenantId);

        Tenant tenant = tenantService.getTenantById(tenantId);
        TenantResponse response = tenantMapper.toResponse(tenant);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get tenant by subdomain
     */
    @GetMapping("/subdomain/{subdomain}")
    @Operation(summary = "Get tenant by subdomain", description = "Retrieves tenant information by subdomain")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantBySubdomain(
            @Parameter(description = "Tenant subdomain") @PathVariable String subdomain) {

        log.debug("Fetching tenant with subdomain: {}", subdomain);

        Tenant tenant = tenantService.getTenantBySubdomain(subdomain);
        TenantResponse response = tenantMapper.toResponse(tenant);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update tenant information
     */
    @PutMapping("/{tenantId}")
    @Operation(summary = "Update tenant", description = "Updates tenant information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {

        log.info("Updating tenant with ID: {}", tenantId);

        // Get existing tenant
        Tenant existingTenant = tenantService.getTenantById(tenantId);

        // Update entity with request data
        tenantMapper.updateEntity(existingTenant, request);

        // Save changes
        Tenant updatedTenant = tenantService.updateTenant(tenantId, existingTenant);

        TenantResponse response = tenantMapper.toResponse(updatedTenant);

        log.info("Successfully updated tenant with ID: {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Tenant updated successfully"));
    }

    /**
     * Delete tenant
     */
    @DeleteMapping("/{tenantId}")
    @Operation(summary = "Delete tenant", description = "Soft deletes a tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {

        log.info("Deleting tenant with ID: {}", tenantId);

        tenantService.deleteTenant(tenantId);

        log.info("Successfully deleted tenant with ID: {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(null, "Tenant deleted successfully"));
    }

    /**
     * List all tenants with pagination
     */
    @GetMapping
    @Operation(summary = "List tenants", description = "Get a paginated list of all tenants")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> listTenants(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Listing tenants - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, sortDirection);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Tenant> tenants = tenantService.getAllTenants(pageable);
        Page<TenantResponse> response = tenants.map(tenantMapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Search tenants
     */
    @GetMapping("/search")
    @Operation(summary = "Search tenants", description = "Search tenants by name or subdomain")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> searchTenants(
            @Parameter(description = "Search term") @RequestParam String query,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        log.debug("Searching tenants with query: {}", query);

        Pageable pageable = PageRequest.of(page, size);
        Page<Tenant> tenants = tenantService.searchTenants(query, pageable);
        Page<TenantResponse> response = tenants.map(tenantMapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get tenants by status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get tenants by status", description = "Retrieves all tenants with a specific status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getTenantsByStatus(
            @Parameter(description = "Tenant status") @PathVariable TenantStatus status) {

        log.debug("Fetching tenants with status: {}", status);

        List<Tenant> tenants = tenantService.getTenantsByStatus(status);
        List<TenantResponse> response = tenants.stream()
                .map(tenantMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Provision tenant resources
     */
    @PostMapping("/{tenantId}/provision")
    @Operation(summary = "Provision tenant", description = "Provisions resources for a tenant (creates schema, initializes data)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant provisioned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tenant already provisioned")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> provisionTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {

        log.info("Provisioning tenant with ID: {}", tenantId);

        tenantService.provisionTenant(tenantId);
        Tenant tenant = tenantService.getTenantById(tenantId);
        TenantResponse response = tenantMapper.toResponse(tenant);

        log.info("Successfully provisioned tenant with ID: {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Tenant provisioned successfully"));
    }

    /**
     * Suspend tenant
     */
    @PostMapping("/{tenantId}/suspend")
    @Operation(summary = "Suspend tenant", description = "Suspends a tenant account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant suspended successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> suspendTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId,
            @Parameter(description = "Suspension reason") @RequestParam String reason) {

        log.info("Suspending tenant with ID: {} for reason: {}", tenantId, reason);

        Tenant tenant = tenantService.suspendTenant(tenantId, reason);
        TenantResponse response = tenantMapper.toResponse(tenant);

        log.info("Successfully suspended tenant with ID: {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Tenant suspended successfully"));
    }

    /**
     * Reactivate tenant
     */
    @PostMapping("/{tenantId}/reactivate")
    @Operation(summary = "Reactivate tenant", description = "Reactivates a suspended tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant reactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<TenantResponse>> reactivateTenant(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {

        log.info("Reactivating tenant with ID: {}", tenantId);

        Tenant tenant = tenantService.reactivateTenant(tenantId);
        TenantResponse response = tenantMapper.toResponse(tenant);

        log.info("Successfully reactivated tenant with ID: {}", tenantId);

        return ResponseEntity.ok(ApiResponse.success(response, "Tenant reactivated successfully"));
    }

    /**
     * Get tenant statistics
     */
    @GetMapping("/{tenantId}/statistics")
    @Operation(summary = "Get tenant statistics", description = "Retrieves usage statistics for a tenant")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantStatistics(
            @Parameter(description = "Tenant ID") @PathVariable UUID tenantId) {

        log.debug("Fetching statistics for tenant ID: {}", tenantId);

        Map<String, Object> statistics = tenantService.getTenantStatistics(tenantId);

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * Check subdomain availability
     */
    @GetMapping("/check-subdomain/{subdomain}")
    @Operation(summary = "Check subdomain availability", description = "Checks if a subdomain is available for registration")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check completed")
    })
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkSubdomainAvailability(
            @Parameter(description = "Subdomain to check") @PathVariable String subdomain) {

        log.debug("Checking availability for subdomain: {}", subdomain);

        boolean available = tenantService.isSubdomainAvailable(subdomain);
        Map<String, Boolean> response = Map.of("available", available);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}