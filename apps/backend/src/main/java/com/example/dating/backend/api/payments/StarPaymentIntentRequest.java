package com.example.dating.backend.api.payments;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StarPaymentIntentRequest(
    @NotBlank String productCode,
    @Min(1) int quantity
) {}
