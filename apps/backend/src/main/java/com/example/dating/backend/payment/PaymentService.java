package com.example.dating.backend.payment;

import com.example.dating.backend.subscription.SubscriptionService;
import com.example.dating.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public PaymentService(PaymentRepository paymentRepository, UserRepository userRepository, SubscriptionService subscriptionService) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    public PaymentResponse createTelegramStarsPayment(Long userId, PaymentCreateRequest request) {
        Payment payment = Payment.builder()
                .user(userRepository.getReferenceById(userId))
                .provider(Payment.Provider.TELEGRAM_STARS)
                .amountStars(request.amountStars())
                .status(Payment.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        paymentRepository.save(payment);
        return new PaymentResponse(payment.getId());
    }

    public void confirmTelegramStarsPayment(Long userId, PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findById(request.paymentId()).orElseThrow();
        if (!payment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("payment mismatch");
        }
        payment.setStatus(Payment.Status.SUCCESS);
        paymentRepository.save(payment);
        subscriptionService.activate(userId, "vip-monthly", OffsetDateTime.now().plusMonths(1));
    }

    public record PaymentCreateRequest(int amountStars) {}
    public record PaymentConfirmRequest(Long paymentId) {}
    public record PaymentResponse(Long id) {}
}
