package ru.gang.datingBot.handler;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.Subscription;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.KeyboardService;
import ru.gang.datingBot.service.ProfileService;
import ru.gang.datingBot.service.SubscriptionService;
import ru.gang.datingBot.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class VipHandler {

    private static final Logger log = LoggerFactory.getLogger(VipHandler.class);

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
        String paymentLink = subscriptionService.getPaymentLink(planType);
        
        if (price <= 0) {
            messageSender.sendTextMessage(chatId, "⚠️ Выбран неверный тарифный план. Пожалуйста, выберите план из предложенных вариантов.");
            return;
        }
        
        try {
            // Создаем подписку в статусе "pending"
            Subscription subscription = subscriptionService.createSubscription(chatId, planType);
            
            // Создаем кнопку со ссылкой на оплату
            InlineKeyboardMarkup paymentKeyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            
            InlineKeyboardButton payButton = new InlineKeyboardButton();
            payButton.setText("💳 Оплатить " + readablePlanType + " за " + price.intValue() + "₽");
            payButton.setUrl(paymentLink);
            
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(payButton);
            rows.add(row);
            
            paymentKeyboard.setKeyboard(rows);
            
            // Отправляем сообщение с ссылкой на оплату
            String confirmationMessage = String.format(
                "🔄 Вы выбрали тарифный план «%s» за %.0f руб.\n\n" +
                "Для активации VIP-статуса, пожалуйста, нажмите кнопку ниже чтобы произвести оплату.\n\n" +
                "После успешной оплаты ваш VIP-статус будет автоматически активирован.",
                readablePlanType, price
            );
            
            messageSender.sendTextMessageWithKeyboard(
                    chatId,
                    confirmationMessage,
                    paymentKeyboard
            );
            
        } catch (Exception e) {
            log.error("Ошибка при создании подписки: {}", e.getMessage(), e);
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
            log.error("Ошибка при подтверждении оплаты: {}", e.getMessage(), e);
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
            log.error("Ошибка при отмене оплаты: {}", e.getMessage(), e);
            messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отмене оплаты. Пожалуйста, попробуйте снова.");
        }
    }
}