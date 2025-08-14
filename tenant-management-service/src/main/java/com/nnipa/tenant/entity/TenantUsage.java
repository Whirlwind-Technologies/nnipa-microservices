package com.nnipa.tenant.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity representing usage metrics for a tenant.
 * Tracks resource consumption for billing and monitoring.
 */
@Entity
@Table(name = "tenant_usage",
        schema = "tenant_registry",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_usage_date", columnNames = {"tenant_id", "usage_date", "metric_name"})
        },
        indexes = {
                @Index(name = "idx_tenant_usage_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tenant_usage_date", columnList = "usage_date"),
                @Index(name = "idx_tenant_usage_metric", columnList = "metric_name")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = "tenant")
public class TenantUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "metric_name", nullable = false, length = 50)
    private String metricName; // e.g., "storage_gb", "api_calls", "users", "datasets"

    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    @Column(name = "metric_unit", length = 20)
    private String metricUnit; // e.g., "GB", "COUNT", "USERS"

    @Column(name = "metric_category", length = 50)
    private String metricCategory; // e.g., "STORAGE", "COMPUTE", "DATA"

    @Column(name = "peak_value")
    private Double peakValue;

    @Column(name = "average_value")
    private Double averageValue;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "sample_count")
    private Integer sampleCount;

    @Column(name = "cost", precision = 10, scale = 4)
    private BigDecimal cost;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "is_billable", nullable = false)
    private Boolean isBillable = true;

    @Column(name = "billing_period", length = 20)
    private String billingPeriod; // HOURLY, DAILY, MONTHLY

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private String metadata;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    /**
     * Check if usage exceeds a threshold
     */
    public boolean exceedsThreshold(Double threshold) {
        return metricValue != null && threshold != null && metricValue > threshold;
    }

    /**
     * Check if this is peak usage
     */
    public boolean isPeakUsage() {
        return metricValue != null &&
                metricValue.equals(peakValue);
    }
}