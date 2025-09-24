package com.example.dating.backend.api.payments;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class MiniAppPaymentService {

    public StarPaymentIntentResponse createIntent(Long telegramId, StarPaymentIntentRequest request) {
        String intentId = UUID.randomUUID().toString();
        return new StarPaymentIntentResponse(
            "https://t.me/pay?mock=true&intent=" + intentId,
            new StarPaymentPayload(request.productCode(), request.quantity(), telegramId, intentId),
            Instant.now()
        );
    }

    public void handleWebhook(String rawBody) {
        // TODO: валидация подписи и пересылка события в billing
    }
}
