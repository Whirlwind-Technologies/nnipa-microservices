package com.nnipa.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable component representing a tenant's address.
 * Used within the Tenant entity.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAddress {

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 2)
    private String country; // ISO 3166-1 alpha-2 country code

    /**
     * Get formatted address as a single string
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();

        if (addressLine1 != null) {
            sb.append(addressLine1);
        }
        if (addressLine2 != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(addressLine2);
        }
        if (city != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(city);
        }
        if (stateProvince != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(stateProvince);
        }
        if (postalCode != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(postalCode);
        }
        if (country != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(country);
        }

        return sb.toString();
    }
}