package com.nnipa.tenant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for tenant address information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Address information")
public class TenantAddressResponse {

    @Schema(description = "First line of address", example = "123 Main Street")
    private String addressLine1;

    @Schema(description = "Second line of address", example = "Suite 500")
    private String addressLine2;

    @Schema(description = "City", example = "New York")
    private String city;

    @Schema(description = "State or province", example = "NY")
    private String stateProvince;

    @Schema(description = "Postal code", example = "10001")
    private String postalCode;

    @Schema(description = "Country code", example = "US")
    private String country;

    @Schema(description = "Formatted full address", example = "123 Main Street, Suite 500, New York, NY 10001, US")
    private String formattedAddress;
}