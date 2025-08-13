package com.nnipa.tenant.dto.request;

import com.nnipa.tenant.enums.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating tenant subscription
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating subscription plan")
public class UpdateSubscriptionRequest {

    @NotNull(message = "Subscription plan is required")
    @Schema(description = "New subscription plan", example = "ENTERPRISE", required = true)
    private SubscriptionPlan plan;

    @Schema(description = "Billing cycle for the subscription", example = "MONTHLY", defaultValue = "MONTHLY")
    private String billingCycle = "MONTHLY";

    @Schema(description = "Whether to auto-renew the subscription", defaultValue = "true")
    private Boolean autoRenew = true;

    @Schema(description = "Promotional code for discount")
    private String promoCode;

    @Schema(description = "Payment method identifier")
    private String paymentMethodId;

    @Schema(description = "Whether to prorate the billing", defaultValue = "true")
    private Boolean prorate = true;

    @Schema(description = "Notes or reason for the change")
    private String notes;
}