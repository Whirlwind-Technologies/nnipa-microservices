package com.nnipa.tenant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing the lifecycle status of a tenant.
 * Tracks the current state of a tenant account.
 */
@Getter
@RequiredArgsConstructor
public enum TenantStatus {

    PENDING("Pending", "Tenant registration pending completion"),
    PROVISIONING("Provisioning", "Tenant resources being provisioned"),
    ACTIVE("Active", "Tenant is active and operational"),
    SUSPENDED("Suspended", "Tenant temporarily suspended"),
    INACTIVE("Inactive", "Tenant marked as inactive"),
    DELETED("Deleted", "Tenant marked for deletion"),
    ARCHIVED("Archived", "Tenant data archived");

    private final String displayName;
    private final String description;

    /**
     * Check if tenant is in an operational state
     */
    public boolean isOperational() {
        return this == ACTIVE;
    }

    /**
     * Check if tenant can be activated
     */
    public boolean canBeActivated() {
        return this == PENDING || this == PROVISIONING ||
                this == SUSPENDED || this == INACTIVE;
    }

    /**
     * Check if tenant can be suspended
     */
    public boolean canBeSuspended() {
        return this == ACTIVE;
    }

    /**
     * Check if tenant data should be retained
     */
    public boolean shouldRetainData() {
        return this != DELETED;
    }
}