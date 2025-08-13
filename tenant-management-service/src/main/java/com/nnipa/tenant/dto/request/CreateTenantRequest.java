package com.nnipa.tenant.dto.request;

import com.nnipa.tenant.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new tenant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a new tenant")
public class CreateTenantRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Legal name of the tenant organization", example = "Acme Corporation", required = true)
    private String name;

    @Size(max = 255, message = "Display name must not exceed 255 characters")
    @Schema(description = "Display name for UI", example = "Acme Corp")
    private String displayName;

    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 3, max = 100, message = "Subdomain must be between 3 and 100 characters")
    @Schema(description = "Unique subdomain for tenant access", example = "acme", required = true)
    private String subdomain;

    @Schema(description = "Subscription plan for the tenant", example = "PROFESSIONAL")
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Brief description of the tenant organization",
            example = "Leading technology company specializing in data analytics")
    private String description;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Contact email is required")
    @Schema(description = "Primary contact email", example = "admin@acme.com", required = true)
    private String contactEmail;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Contact phone number in E.164 format", example = "+12125551234")
    private String contactPhone;

    @Size(max = 255, message = "Website URL must not exceed 255 characters")
    @Pattern(regexp = "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$",
            message = "Invalid website URL format")
    @Schema(description = "Company website URL", example = "https://www.acme.com")
    private String website;

    @Schema(description = "Tenant address information")
    private TenantAddressRequest address;

    @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$", message = "Invalid timezone format")
    @Schema(description = "Timezone for the tenant", example = "America/New_York", defaultValue = "UTC")
    private String timezone = "UTC";

    @Pattern(regexp = "^[a-z]{2}_[A-Z]{2}$", message = "Invalid locale format")
    @Schema(description = "Locale for the tenant", example = "en_US", defaultValue = "en_US")
    private String locale = "en_US";

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    @Schema(description = "Currency code (ISO 4217)", example = "USD", defaultValue = "USD")
    private String currency = "USD";

    @Schema(description = "Admin user details for the tenant")
    private CreateAdminUserRequest adminUser;

    @Schema(description = "Whether to automatically provision resources", defaultValue = "true")
    private Boolean autoProvision = true;

    @Schema(description = "Whether to send welcome email", defaultValue = "true")
    private Boolean sendWelcomeEmail = true;
}