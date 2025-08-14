package com.nnipa.tenant.entity;

import com.nnipa.tenant.enums.FeatureFlag;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;

/**
 * Entity representing feature flags enabled for a specific tenant.
 * Controls which features are available to each tenant.
 */
@Entity
@Table(name = "tenant_features",
        schema = "tenant_registry",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_feature", columnNames = {"tenant_id", "feature"})
        },
        indexes = {
                @Index(name = "idx_tenant_features_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tenant_features_enabled", columnList = "enabled")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = "tenant")
public class TenantFeature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature", nullable = false, length = 50)
    private FeatureFlag feature;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "configuration", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private String configuration;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Check if this feature is currently active
     */
    public boolean isActive() {
        if (!enabled || deletedAt != null) {
            return false;
        }
        return expiresAt == null || !expiresAt.isBefore(Instant.now());
    }

    /**
     * Check if this feature is expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}