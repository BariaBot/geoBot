package ru.gang.datingBot.handler;

import lombok.RequiredArgsConstructor;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.Subscription;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.KeyboardService;
import ru.gang.datingBot.service.ProfileService;
import ru.gang.datingBot.service.SubscriptionService;
import ru.gang.datingBot.service.UserService;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
public class VipHandler {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final UserStateManager stateManager;
    private final MessageSender messageSender;
    private final KeyboardService keyboardService;
    private final ProfileService profileService;

    /**
     * Обрабатывает команду /vip - показывает информацию о VIP-статусе и опции покупки
     */
    public void processVipCommand(Long chatId) {
        User user = userService.getUserByTelegramId(chatId);
        if (user == null) {
            messageSender.sendTextMessage(chatId, "⚠️ Ваш профиль не найден. Пожалуйста, обновите свой профиль с помощью команды /edit_profile");
            return;
        }

        Map<String, Object> vipInfo = subscriptionService.getVipInfo(chatId);
        boolean isVipActive = (boolean) vipInfo.getOrDefault("isVip", false);
        
        String vipMessage = profileService.formatVipInfo(
                user,
                isVipActive,
                isVipActive ? (LocalDateTime) vipInfo.get("expiresAt") : null,
                isVipActive ? (String) vipInfo.get("planType") : null
        );

        if (isVipActive) {
            messageSender.sendTextMessage(chatId, vipMessage);
        } else {
            // Показываем опции покупки для неактивных VIP
            messageSender.sendTextMessageWithKeyboard(
                    chatId,
                    vipMessage,
                    keyboardService.createVipPlansKeyboard()
            );
        }
    }

    /**
     * Обрабатывает выбор плана VIP-подписки
     */
    public void processVipPlanSelection(Long chatId, String planType) {
        Double price = subscriptionService.getSubscriptionPrice(planType);
        String readablePlanType = profileService.getReadablePlanType(planType);
        
        if (price <= 0) {
            messageSender.sendTextMessage(chatId, "⚠️ Выбран неверный тарифный план. Пожалуйста, выберите план из предложенных вариантов.");
            return;
        }
        
        try {
            // Создаем подписку в статусе "pending"
            Subscription subscription = subscriptionService.createSubscription(chatId, planType);
            
            // Отправляем подтверждение и информацию об оплате (эмуляция)
            String confirmationMessage = String.format(
                "🔄 Вы выбрали тарифный план «%s» за %.0f руб.\n\n" +
                "Для завершения подписки, пожалуйста, подтвердите оплату, нажав на кнопку ниже.\n\n" +
                "ID заказа: %d",
                readablePlanType, price, subscription.getId()
            );
            
            messageSender.sendTextMessageWithKeyboard(
                    chatId,
                    confirmationMessage,
                    keyboardService.createPaymentConfirmationKeyboard(subscription.getId())
            );
            
        } catch (Exception e) {
            System.out.println("DEBUG: Ошибка при создании подписки: " + e.getMessage());
            messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при оформлении подписки. Пожалуйста, попробуйте позже.");
        }
    }

    /**
     * Обрабатывает подтверждение оплаты VIP-подписки (эмуляция)
     */
    public void processPaymentConfirmation(Long chatId, Long subscriptionId) {
        try {
            // Эмулируем транзакцию
            String transactionId = "tx_" + System.currentTimeMillis();
            String paymentMethod = "card";
            
            // Активируем подписку
            Subscription subscription = subscriptionService.confirmPayment(subscriptionId, transactionId, paymentMethod);
            
            // Отправляем подтверждение активации VIP
            String successMessage = String.format(
                "✅ Поздравляем! Ваш VIP-статус активирован до %s\n\n" +
                "👑 Теперь ваш профиль будет отображаться с пометкой VIP\n" +
                "📱 Другие пользователи увидят ваш username для быстрой связи\n" +
                "🔍 Вы будете отображаться выше в результатах поиска",
                subscription.getEndDate().toLocalDate()
            );
            
            messageSender.sendTextMessageWithKeyboard(
                    chatId,
                    successMessage,
                    keyboardService.createMainKeyboard()
            );
            
        } catch (Exception e) {
            System.out.println("DEBUG: Ошибка при подтверждении оплаты: " + e.getMessage());
            messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при активации VIP-статуса. Пожалуйста, обратитесь в поддержку.");
        }
    }

    /**
     * Обрабатывает отмену оплаты
     */
    public void processPaymentCancel(Long chatId, Long subscriptionId) {
        try {
            subscriptionService.failPayment(subscriptionId, "Отменено пользователем");
            
            messageSender.sendTextMessageWithKeyboard(
                    chatId,
                    "🛑 Оплата отменена. Вы можете приобрести VIP-статус в любое время с помощью команды /vip",
                    keyboardService.createMainKeyboard()
            );
            
        } catch (Exception e) {
            System.out.println("DEBUG: Ошибка при отмене оплаты: " + e.getMessage());
            messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отмене оплаты. Пожалуйста, попробуйте снова.");
        }
    }
}