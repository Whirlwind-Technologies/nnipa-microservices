package com.nnipa.tenant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing different subscription plans available for tenants.
 * Each plan has different resource limits and features.
 */
@Getter
@RequiredArgsConstructor
public enum SubscriptionPlan {

    FREE("Free", "Free tier with basic features", 0.0, 30),
    STARTER("Starter", "Small teams and startups", 49.99, 0),
    PROFESSIONAL("Professional", "Growing organizations", 199.99, 0),
    ENTERPRISE("Enterprise", "Large organizations with custom needs", 999.99, 0),
    EDUCATION("Education", "Special pricing for educational institutions", 99.99, 0);

    private final String displayName;
    private final String description;
    private final Double basePrice;
    private final Integer trialDays;

    /**
     * Check if this plan is a paid plan
     */
    public boolean isPaid() {
        return this != FREE;
    }

    /**
     * Check if this plan includes trial period
     */
    public boolean hasTrial() {
        return trialDays != null && trialDays > 0;
    }

    /**
     * Get the plan by name (case-insensitive)
     */
    public static SubscriptionPlan fromString(String plan) {
        for (SubscriptionPlan sp : SubscriptionPlan.values()) {
            if (sp.name().equalsIgnoreCase(plan)) {
                return sp;
            }
        }
        throw new IllegalArgumentException("Unknown subscription plan: " + plan);
    }
}