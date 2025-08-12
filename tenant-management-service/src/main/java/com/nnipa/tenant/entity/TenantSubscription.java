package com.nnipa.tenant.entity;

import com.nnipa.tenant.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing subscription history for a tenant.
 * Tracks all subscription changes and billing information.
 */
@Entity
@Table(name = "tenant_subscriptions",
        schema = "tenant_registry",
        indexes = {
                @Index(name = "idx_tenant_subs_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tenant_subs_status", columnList = "status"),
                @Index(name = "idx_tenant_subs_dates", columnList = "start_date, end_date")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = "tenant")
public class TenantSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private SubscriptionPlan plan;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, CANCELLED, EXPIRED, PENDING

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "trial_start_date")
    private Instant trialStartDate;

    @Column(name = "trial_end_date")
    private Instant trialEndDate;

    @Column(name = "billing_cycle", length = 20)
    private String billingCycle; // MONTHLY, YEARLY, CUSTOM

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /**
     * Check if subscription is currently active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status) &&
                (endDate == null || endDate.isAfter(Instant.now()));
    }

    /**
     * Check if subscription is in trial period
     */
    public boolean isInTrial() {
        return trialEndDate != null &&
                trialEndDate.isAfter(Instant.now()) &&
                (trialStartDate == null || trialStartDate.isBefore(Instant.now()));
    }

    /**
     * Calculate the final price after discounts
     */
    public BigDecimal calculateFinalPrice() {
        if (price == null) return BigDecimal.ZERO;

        BigDecimal finalAmount = price;

        if (discountAmount != null) {
            finalAmount = finalAmount.subtract(discountAmount);
        } else if (discountPercentage != null && discountPercentage > 0) {
            BigDecimal discount = price.multiply(
                    BigDecimal.valueOf(discountPercentage).divide(BigDecimal.valueOf(100))
            );
            finalAmount = finalAmount.subtract(discount);
        }

        return finalAmount.max(BigDecimal.ZERO);
    }
}