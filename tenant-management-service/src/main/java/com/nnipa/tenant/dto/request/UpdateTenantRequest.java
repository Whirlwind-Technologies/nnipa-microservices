package com.nnipa.tenant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating tenant information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating tenant information")
public class UpdateTenantRequest {

    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Updated organization name", example = "Acme Corporation Inc.")
    private String name;

    @Size(max = 255, message = "Display name must not exceed 255 characters")
    @Schema(description = "Updated display name", example = "Acme Inc.")
    private String displayName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Updated description")
    private String description;

    @Email(message = "Invalid email format")
    @Schema(description = "Updated contact email", example = "contact@acme.com")
    private String contactEmail;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Schema(description = "Updated phone number")
    private String contactPhone;

    @Size(max = 255, message = "Website URL must not exceed 255 characters")
    @Pattern(regexp = "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$",
            message = "Invalid website URL format")
    @Schema(description = "Updated website URL")
    private String website;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    @Schema(description = "URL to tenant's logo image")
    private String logoUrl;

    @Schema(description = "Updated address information")
    private TenantAddressRequest address;

    @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$", message = "Invalid timezone format")
    @Schema(description = "Updated timezone", example = "Europe/London")
    private String timezone;

    @Pattern(regexp = "^[a-z]{2}_[A-Z]{2}$", message = "Invalid locale format")
    @Schema(description = "Updated locale", example = "fr_FR")
    private String locale;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    @Schema(description = "Updated currency code", example = "EUR")
    private String currency;
}