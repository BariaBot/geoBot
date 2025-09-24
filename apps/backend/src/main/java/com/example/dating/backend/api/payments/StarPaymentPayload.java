package com.example.dating.backend.api.payments;

public record StarPaymentPayload(
    String productCode,
    int quantity,
    Long telegramUserId,
    String intentId
) {}
