package com.example.dating.backend.payment;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/telegram-stars/create")
    public PaymentService.PaymentResponse create(@RequestBody PaymentService.PaymentCreateRequest request, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return paymentService.createTelegramStarsPayment(userId, request);
    }

    @PostMapping("/telegram-stars/confirm")
    public void confirm(@RequestBody PaymentService.PaymentConfirmRequest request, Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        paymentService.confirmTelegramStarsPayment(userId, request);
    }
}
