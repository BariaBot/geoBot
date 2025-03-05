package ru.gang.datingBot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gang.datingBot.bot.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.UserService;

import java.util.List;

/**
 * Обработчик сообщений с геолокацией
 */
public class LocationHandler {

  private final UserService userService;
  private final UserStateManager stateManager;
  private final MessageSender messageSender;
  private final KeyboardService keyboardService;
  // Добавляем ссылку на CallbackQueryHandler
  private CallbackQueryHandler callbackQueryHandler;

  public LocationHandler(
          UserService userService,
          UserStateManager stateManager,
          MessageSender messageSender) {
    this.userService = userService;
    this.stateManager = stateManager;
    this.messageSender = messageSender;
    this.keyboardService = new KeyboardService();
  }

  /**
   * Устанавливает ссылку на CallbackQueryHandler
   * Необходимо вызвать этот метод после создания всех обработчиков
   */
  public void setCallbackQueryHandler(CallbackQueryHandler callbackQueryHandler) {
    this.callbackQueryHandler = callbackQueryHandler;
  }

  /**
   * Обрабатывает сообщения с геолокацией
   */
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

      // Удаляем сообщение "Отправьте свою геолокацию..."
      messageSender.deleteMessage(chatId, messageId);

      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "📍 Ваше местоположение обновлено! Мы ищем для вас людей поблизости...",
              keyboardService.createMainKeyboard());

      // Поиск пользователей поблизости с учетом фильтров
      List<User> nearbyUsers = userService.findNearbyUsers(chatId, latitude, longitude, radius);
      System.out.println("DEBUG: Найдено пользователей поблизости: " + (nearbyUsers != null ? nearbyUsers.size() : 0));

      // Кэшируем результаты поиска
      stateManager.cacheNearbyUsers(chatId, nearbyUsers);

      // Отображаем информацию о найденных пользователях
      showNearbyUsers(chatId, nearbyUsers);
    } else {
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "⚠️ Пожалуйста, выберите время и радиус перед отправкой геолокации.",
              keyboardService.createTimeSelectionKeyboard());
    }
  }

  /**
   * Отображает информацию о найденных пользователях
   */
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

    // Показываем информацию о количестве найденных пользователей
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "🔍 Найдено " + nearbyUsers.size() + " человек поблизости!",
            keyboardService.createMainKeyboard());

    // Отобразим первого пользователя
    if (callbackQueryHandler != null) {
      System.out.println("DEBUG: Вызываем показ профиля первого пользователя");
      // Небольшая задержка перед показом профиля
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