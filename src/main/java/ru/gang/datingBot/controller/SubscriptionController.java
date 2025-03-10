package ru.gang.datingBot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.gang.datingBot.model.Subscription;
import ru.gang.datingBot.service.SubscriptionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ResponseEntity<Map<String, Double>> getSubscriptionPlans() {
        return ResponseEntity.ok(subscriptionService.getSubscriptionPlans());
    }

    @PostMapping("/{telegramId}/create")
    public ResponseEntity<Subscription> createSubscription(
            @PathVariable Long telegramId, 
            @RequestParam String planType) {
        
        try {
            Subscription subscription = subscriptionService.createSubscription(telegramId, planType);
            return ResponseEntity.ok(subscription);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{subscriptionId}/confirm")
    public ResponseEntity<Subscription> confirmPayment(
            @PathVariable Long subscriptionId,
            @RequestParam String transactionId,
            @RequestParam String paymentMethod) {
        
        try {
            Subscription subscription = subscriptionService.confirmPayment(
                    subscriptionId, transactionId, paymentMethod);
            return ResponseEntity.ok(subscription);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{subscriptionId}/fail")
    public ResponseEntity<Subscription> failPayment(
            @PathVariable Long subscriptionId,
            @RequestParam String reason) {
        
        try {
            Subscription subscription = subscriptionService.failPayment(subscriptionId, reason);
            return ResponseEntity.ok(subscription);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{telegramId}/status")
    public ResponseEntity<Map<String, Object>> getVipStatus(@PathVariable Long telegramId) {
        Map<String, Object> status = subscriptionService.getVipInfo(telegramId);
        return ResponseEntity.ok(status);
    }
}