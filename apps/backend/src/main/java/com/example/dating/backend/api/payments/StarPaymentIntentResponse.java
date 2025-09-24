package com.example.dating.backend.api.payments;

import java.time.Instant;

public record StarPaymentIntentResponse(
    String invoiceUrl,
    StarPaymentPayload payload,
    Instant createdAt
) {}
