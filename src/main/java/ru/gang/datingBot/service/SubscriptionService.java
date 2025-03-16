package ru.gang.datingBot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gang.datingBot.model.Subscription;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.SubscriptionRepository;
import ru.gang.datingBot.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    
    // Стоимость подписок в рублях
    private static final Map<String, Double> SUBSCRIPTION_PRICES = new HashMap<>();
    static {
        SUBSCRIPTION_PRICES.put("week", 490.0);
        SUBSCRIPTION_PRICES.put("2weeks", 890.0);
        SUBSCRIPTION_PRICES.put("month", 1590.0);
    }
    
    // Продолжительность в днях
    private static final Map<String, Integer> SUBSCRIPTION_DURATIONS = new HashMap<>();
    static {
        SUBSCRIPTION_DURATIONS.put("week", 7);
        SUBSCRIPTION_DURATIONS.put("2weeks", 14);
        SUBSCRIPTION_DURATIONS.put("month", 30);
    }
    
    // Ссылки на оплату
    private static final Map<String, String> PAYMENT_LINKS = new HashMap<>();
    static {
        PAYMENT_LINKS.put("week", "https://t.me/tribute/app?startapp=dmPw");
        PAYMENT_LINKS.put("2weeks", "https://t.me/tribute/app?startapp=dmTO");
        PAYMENT_LINKS.put("month", "https://t.me/tribute/app?startapp=dmTN");
    }
    
    /**
     * Создает новую подписку для пользователя
     */
    @Transactional
    public Subscription createSubscription(Long telegramId, String planType) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        if (!SUBSCRIPTION_PRICES.containsKey(planType)) {
            throw new IllegalArgumentException("Недопустимый тип плана: " + planType);
        }
        
        Double amount = SUBSCRIPTION_PRICES.get(planType);
        int durationDays = SUBSCRIPTION_DURATIONS.get(planType);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(durationDays);
        
        // Если у пользователя уже есть активная подписка, продлеваем ее
        LocalDateTime startDate = now;
        List<Subscription> activeSubscriptions = subscriptionRepository.findActiveSubscriptions(user, now);
        if (!activeSubscriptions.isEmpty()) {
            Subscription latestSubscription = activeSubscriptions.get(0);
            startDate = latestSubscription.getEndDate();
            endDate = startDate.plusDays(durationDays);
        }
        
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlanType(planType);
        subscription.setAmount(amount);
        subscription.setStatus("pending");
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        
        return subscriptionRepository.save(subscription);
    }
    
    /**
     * Подтверждает оплату подписки и активирует VIP-статус
     */
    @Transactional
    public Subscription confirmPayment(Long subscriptionId, String transactionId, String paymentMethod) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Подписка не найдена"));
        
        if (!"pending".equals(subscription.getStatus())) {
            throw new IllegalStateException("Подписка уже обработана");
        }
        
        subscription.setStatus("completed");
        subscription.setTransactionId(transactionId);
        subscription.setPaymentMethod(paymentMethod);
        subscription.setUpdatedAt(LocalDateTime.now());
        
        User user = subscription.getUser();
        user.setIsVip(true);
        user.setVipExpiresAt(subscription.getEndDate());
        userRepository.save(user);
        
        return subscriptionRepository.save(subscription);
    }
    
    /**
     * Отмечает подписку как неудачную
     */
    @Transactional
    public Subscription failPayment(Long subscriptionId, String reason) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Подписка не найдена"));
        
        if (!"pending".equals(subscription.getStatus())) {
            throw new IllegalStateException("Подписка уже обработана");
        }
        
        subscription.setStatus("failed");
        subscription.setUpdatedAt(LocalDateTime.now());
        
        return subscriptionRepository.save(subscription);
    }
    
    /**
     * Проверяет наличие активной VIP-подписки у пользователя
     */
    public boolean hasActiveVip(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        if (user == null) return false;
        
        return user.getIsVip() != null && user.getIsVip() && 
               user.getVipExpiresAt() != null && 
               user.getVipExpiresAt().isAfter(LocalDateTime.now());
    }
    
    /**
     * Получает информацию о VIP-статусе пользователя
     */
    public Map<String, Object> getVipInfo(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElse(null);
        Map<String, Object> result = new HashMap<>();
        
        if (user == null) {
            result.put("isVip", false);
            return result;
        }
        
        boolean isVipActive = user.getIsVip() != null && user.getIsVip() && 
                             user.getVipExpiresAt() != null && 
                             user.getVipExpiresAt().isAfter(LocalDateTime.now());
        
        result.put("isVip", isVipActive);
        
        if (isVipActive) {
            result.put("expiresAt", user.getVipExpiresAt());
            
            // Находим последнюю подписку
            subscriptionRepository.findTopByUserOrderByEndDateDesc(user)
                .ifPresent(subscription -> {
                    result.put("planType", subscription.getPlanType());
                    result.put("startDate", subscription.getStartDate());
                });
        }
        
        return result;
    }
    
    /**
     * Возвращает стоимость подписки по типу
     */
    public Double getSubscriptionPrice(String planType) {
        return SUBSCRIPTION_PRICES.getOrDefault(planType, 0.0);
    }
    
    /**
     * Получает все доступные планы подписки
     */
    public Map<String, Double> getSubscriptionPlans() {
        return new HashMap<>(SUBSCRIPTION_PRICES);
    }
    
    /**
     * Получает ссылку на оплату для выбранного плана подписки
     */
    public String getPaymentLink(String planType) {
        return PAYMENT_LINKS.getOrDefault(planType, "");
    }
    
    /**
     * Запланированная задача для деактивации истекших VIP-статусов
     */
    @Scheduled(cron = "0 0 * * * *") // Каждый час
    @Transactional
    public void deactivateExpiredVipStatuses() {
        System.out.println("Проверка истекших VIP-статусов...");
        LocalDateTime now = LocalDateTime.now();
        
        List<User> users = userRepository.findAll();
        int count = 0;
        
        for (User user : users) {
            if (user.getIsVip() != null && user.getIsVip() && 
                user.getVipExpiresAt() != null && user.getVipExpiresAt().isBefore(now)) {
                user.setIsVip(false);
                userRepository.save(user);
                count++;
            }
        }
        
        System.out.println("Деактивировано " + count + " истекших VIP-статусов");
    }
}