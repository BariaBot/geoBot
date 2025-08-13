package com.example.dating.backend.subscription;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public List<SubscriptionService.PlanDto> plans() {
        return subscriptionService.getAvailablePlans();
    }

    @GetMapping("/status")
    public SubscriptionService.SubscriptionStatusDto status(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return subscriptionService.getStatus(userId);
    }
}
