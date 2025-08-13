package com.example.dating.backend.subscription;

import com.example.dating.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    public List<PlanDto> getAvailablePlans() {
        return List.of(new PlanDto("vip-monthly", 100));
    }

    public SubscriptionStatusDto getStatus(Long userId) {
        return subscriptionRepository
                .findTopByUserIdAndStatusOrderByExpiresAtDesc(userId, Subscription.Status.ACTIVE)
                .map(s -> new SubscriptionStatusDto(true, s.getExpiresAt()))
                .orElseGet(() -> new SubscriptionStatusDto(false, null));
    }

    public void activate(Long userId, String planCode, OffsetDateTime expiresAt) {
        Subscription sub = Subscription.builder()
                .user(userRepository.getReferenceById(userId))
                .planCode(planCode)
                .status(Subscription.Status.ACTIVE)
                .startedAt(OffsetDateTime.now())
                .expiresAt(expiresAt)
                .build();
        subscriptionRepository.save(sub);
    }

    public record PlanDto(String code, int priceStars) {}
    public record SubscriptionStatusDto(boolean active, OffsetDateTime expiresAt) {}
}
