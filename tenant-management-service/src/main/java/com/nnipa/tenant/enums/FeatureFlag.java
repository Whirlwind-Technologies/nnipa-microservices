package com.nnipa.tenant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing feature flags that can be enabled/disabled per tenant.
 * Used for controlling access to specific platform features.
 */
@Getter
@RequiredArgsConstructor
public enum FeatureFlag {

    // Basic Features
    BASIC_ANALYTICS("Basic Analytics", "Access to basic analytical tools"),
    DATA_EXPORT("Data Export", "Ability to export data"),
    API_ACCESS("API Access", "Access to platform APIs"),

    // Advanced Features
    ADVANCED_ANALYTICS("Advanced Analytics", "Access to advanced analytical tools"),
    CUSTOM_BRANDING("Custom Branding", "Ability to customize branding"),
    PRIORITY_SUPPORT("Priority Support", "Access to priority customer support"),

    // Enterprise Features
    ML_MODELS("Machine Learning Models", "Access to ML model training and deployment"),
    WHITE_LABELING("White Labeling", "Complete white-label solution"),
    DEDICATED_SUPPORT("Dedicated Support", "Dedicated support team"),
    CUSTOM_INTEGRATIONS("Custom Integrations", "Custom third-party integrations"),
    SSO_INTEGRATION("SSO Integration", "Single Sign-On integration"),
    AUDIT_LOGS("Audit Logs", "Detailed audit logging"),

    // Data Features
    UNLIMITED_STORAGE("Unlimited Storage", "No storage limits"),
    DATA_RETENTION_CONTROL("Data Retention Control", "Custom data retention policies"),
    CROSS_TENANT_SHARING("Cross-Tenant Sharing", "Share data with other tenants"),

    // Survey Features
    ADVANCED_SURVEYS("Advanced Surveys", "Advanced survey features"),
    SURVEY_LOGIC("Survey Logic", "Complex survey logic and branching"),
    SURVEY_TEMPLATES("Survey Templates", "Access to premium survey templates"),

    // Visualization Features
    CUSTOM_DASHBOARDS("Custom Dashboards", "Create custom dashboards"),
    ADVANCED_VISUALIZATIONS("Advanced Visualizations", "Advanced visualization types"),
    PUBLIC_DASHBOARDS("Public Dashboards", "Publish dashboards publicly");

    private final String displayName;
    private final String description;

    /**
     * Check if this is a basic feature (included in free plan)
     */
    public boolean isBasicFeature() {
        return this == BASIC_ANALYTICS || this == DATA_EXPORT || this == API_ACCESS;
    }

    /**
     * Check if this is an enterprise-only feature
     */
    public boolean isEnterpriseFeature() {
        return this == ML_MODELS || this == WHITE_LABELING ||
                this == DEDICATED_SUPPORT || this == CUSTOM_INTEGRATIONS;
    }
}