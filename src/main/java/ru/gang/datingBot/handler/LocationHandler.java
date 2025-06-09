package ru.gang.datingBot.handler;

import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gang.datingBot.service.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.UserService;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocationHandler {

  private final UserService userService;
  private final UserStateManager stateManager;
  private final MessageSender messageSender;
  private final KeyboardService keyboardService;
    
  @Setter
  private CallbackQueryHandler callbackQueryHandler;

  public void processLocationMessage(Long chatId, double latitude, double longitude, Integer messageId, Update update) {
    Integer duration = stateManager.getLocationDuration(chatId);
    Integer radius = stateManager.getSearchRadius(chatId);

    if (duration != null && radius != null) {
      var from = update.getMessage().getFrom();

      String telegramUsername = (from.getUserName() != null) ? from.getUserName() : null;
      String firstName = (from.getFirstName() != null) ? from.getFirstName() : null;
      String lastName = (from.getLastName() != null) ? from.getLastName() : null;
      String phoneNumber = (update.getMessage().hasContact()) ? update.getMessage().getContact().getPhoneNumber() : null;

      userService.updateUserLocation(chatId, latitude, longitude, duration, radius, telegramUsername, firstName, lastName, phoneNumber);

      messageSender.deleteMessage(chatId, messageId);

      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "📍 Ваше местоположение обновлено! Мы ищем для вас людей поблизости...",
              keyboardService.createMainKeyboard());

      List<User> nearbyUsers = userService.findNearbyUsers(chatId, latitude, longitude, radius);
      System.out.println("DEBUG: Найдено пользователей поблизости: " + (nearbyUsers != null ? nearbyUsers.size() : 0));

      stateManager.cacheNearbyUsers(chatId, nearbyUsers);

      showNearbyUsers(chatId, nearbyUsers);
    } else {
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "⚠️ Пожалуйста, выберите время и радиус перед отправкой геолокации.",
              keyboardService.createTimeSelectionKeyboard());
    }
  }

  private void showNearbyUsers(Long chatId, List<User> nearbyUsers) {
    if (nearbyUsers == null || nearbyUsers.isEmpty()) {
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "😔 На данный момент никого поблизости не найдено, попробуйте позже.\n\n" +
                      "📍 У вас активна геолокация на " + stateManager.getLocationDuration(chatId) +
                      " часов. Если кто-то окажется рядом, мы вам сообщим!",
              keyboardService.createMainKeyboard());
      return;
    }

    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "🔍 Найдено " + nearbyUsers.size() + " человек поблизости!",
            keyboardService.createMainKeyboard());

    if (callbackQueryHandler != null) {
      System.out.println("DEBUG: Вызываем показ профиля первого пользователя");
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      callbackQueryHandler.showCurrentNearbyUser(chatId);
    } else {
      System.out.println("DEBUG: callbackQueryHandler равен null, не могу показать профиль");
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "⚠️ Не удалось отобразить профиль пользователя. Пожалуйста, обновите геолокацию позже.",
              keyboardService.createMainKeyboard());
    }
  }
}