package com.nnipa.tenant.entity;

import com.nnipa.tenant.enums.SubscriptionPlan;
import com.nnipa.tenant.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Main entity representing a tenant in the multi-tenant system.
 * Each tenant represents an organization using the platform.
 */
@Entity
@Table(name = "tenants",
        schema = "tenant_registry",
        indexes = {
                @Index(name = "idx_tenant_subdomain", columnList = "subdomain", unique = true),
                @Index(name = "idx_tenant_status", columnList = "status"),
                @Index(name = "idx_tenant_plan", columnList = "subscription_plan"),
                @Index(name = "idx_tenant_created", columnList = "created_at")
        })
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(exclude = {"features", "configurations", "subscriptions", "usageRecords"})
public class Tenant extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "subdomain", nullable = false, unique = true, length = 100)
    private String subdomain;

    @Column(name = "schema_name", nullable = false, unique = true, length = 100)
    private String schemaName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 20)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Embedded
    private TenantAddress address;

    @Column(name = "timezone", length = 50)
    private String timezone = "UTC";

    @Column(name = "locale", length = 10)
    private String locale = "en_US";

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    // Subscription dates
    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "subscription_ends_at")
    private Instant subscriptionEndsAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Resource limits (cached from subscription plan)
    @Column(name = "max_users")
    private Integer maxUsers = 10;

    @Column(name = "max_storage_gb")
    private Integer maxStorageGb = 25;

    @Column(name = "max_datasets")
    private Integer maxDatasets = 100;

    // Usage metrics (cached for performance)
    @Column(name = "current_users")
    private Integer currentUsers = 0;

    @Column(name = "current_storage_gb")
    private Double currentStorageGb = 0.0;

    @Column(name = "current_datasets")
    private Integer currentDatasets = 0;

    // Metadata
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags;

    // Relationships
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @SQLRestriction("deleted_at IS NULL")
    private Set<TenantFeature> features = new HashSet<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<TenantConfiguration> configurations = new HashSet<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private Set<TenantSubscription> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<TenantUsage> usageRecords = new HashSet<>();

    // Business methods

    /**
     * Check if tenant is active and operational
     */
    public boolean isActive() {
        return status == TenantStatus.ACTIVE && deletedAt == null;
    }

    /**
     * Check if tenant is in trial period
     */
    public boolean isInTrial() {
        return trialEndsAt != null && trialEndsAt.isAfter(Instant.now());
    }

    /**
     * Check if tenant subscription is expired
     */
    public boolean isSubscriptionExpired() {
        return subscriptionEndsAt != null && subscriptionEndsAt.isBefore(Instant.now());
    }

    /**
     * Check if tenant has reached user limit
     */
    public boolean hasReachedUserLimit() {
        return maxUsers > 0 && currentUsers >= maxUsers;
    }

    /**
     * Check if tenant has reached storage limit
     */
    public boolean hasReachedStorageLimit() {
        return maxStorageGb > 0 && currentStorageGb >= maxStorageGb;
    }

    /**
     * Check if tenant has reached dataset limit
     */
    public boolean hasReachedDatasetLimit() {
        return maxDatasets > 0 && currentDatasets >= maxDatasets;
    }
}