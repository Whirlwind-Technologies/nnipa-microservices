package com.nnipa.tenant.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for tenant address information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Address information for a tenant")
public class TenantAddressRequest {

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    @Schema(description = "First line of the address", example = "123 Main Street")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    @Schema(description = "Second line of the address", example = "Suite 500")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Schema(description = "City name", example = "New York")
    private String city;

    @Size(max = 100, message = "State/Province must not exceed 100 characters")
    @Schema(description = "State or province", example = "NY")
    private String stateProvince;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Schema(description = "Postal or ZIP code", example = "10001")
    private String postalCode;

    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a 2-letter ISO code")
    @Schema(description = "Country code (ISO 3166-1 alpha-2)", example = "US")
    private String country;
}