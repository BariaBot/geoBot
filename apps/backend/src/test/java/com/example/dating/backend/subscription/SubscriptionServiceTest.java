package com.example.dating.backend.subscription;

import com.example.dating.backend.payment.PaymentService;
import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SubscriptionServiceTest {

    @Autowired
    PaymentService paymentService;
    @Autowired
    SubscriptionService subscriptionService;
    @Autowired
    UserRepository userRepository;

    @Test
    void confirmPaymentActivatesSubscription() {
        User user = userRepository.save(User.builder().telegramId(10L).username("u").createdAt(OffsetDateTime.now()).build());
        var payment = paymentService.createTelegramStarsPayment(user.getId(), new PaymentService.PaymentCreateRequest(100));
        paymentService.confirmTelegramStarsPayment(user.getId(), new PaymentService.PaymentConfirmRequest(payment.id()));
        var status = subscriptionService.getStatus(user.getId());
        assertTrue(status.active());
        assertNotNull(status.expiresAt());
    }
}
