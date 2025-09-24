package com.example.dating.backend.api.payments;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class MiniAppPaymentController {

    private final MiniAppPaymentService paymentService;

    public MiniAppPaymentController(MiniAppPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/stars/intents")
    public ResponseEntity<StarPaymentIntentResponse> createStarsIntent(
        @RequestHeader("x-telegram-user-id") Long telegramUserId,
        @Valid @RequestBody StarPaymentIntentRequest request
    ) {
        return ResponseEntity.ok(paymentService.createIntent(telegramUserId, request));
    }

    @PostMapping("/stars/webhook")
    public ResponseEntity<WebhookAck> handleWebhook(@RequestBody String payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok(new WebhookAck(true));
    }
}
