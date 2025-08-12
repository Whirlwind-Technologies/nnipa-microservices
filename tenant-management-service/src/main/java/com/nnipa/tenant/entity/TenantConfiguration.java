package com.nnipa.tenant.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing configuration settings for a specific tenant.
 * Stores key-value pairs for tenant-specific configurations.
 */
@Entity
@Table(name = "tenant_configurations",
        schema = "tenant_registry",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_config_key", columnNames = {"tenant_id", "config_key"})
        },
        indexes = {
                @Index(name = "idx_tenant_configs_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tenant_configs_category", columnList = "category")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = "tenant")
public class TenantConfiguration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "data_type", length = 20)
    private String dataType; // STRING, NUMBER, BOOLEAN, JSON

    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted = false;

    @Column(name = "is_sensitive", nullable = false)
    private Boolean isSensitive = false;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private String validationRules;

    /**
     * Get the effective value (current value or default)
     */
    public String getEffectiveValue() {
        return configValue != null ? configValue : defaultValue;
    }

    /**
     * Check if this configuration has a non-default value
     */
    public boolean hasCustomValue() {
        return configValue != null && !configValue.equals(defaultValue);
    }
}