package ru.gang.datingBot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;
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
  // Добавляем ссылку на CallbackQueryHandler
  private CallbackQueryHandler callbackQueryHandler;

  public LocationHandler(
          UserService userService,
          UserStateManager stateManager,
          MessageSender messageSender) {
    this.userService = userService;
    this.stateManager = stateManager;
    this.messageSender = messageSender;
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

      messageSender.sendTextMessage(chatId, "📍 Ваше местоположение обновлено! Мы ищем для вас людей поблизости...");
      
      // Поиск пользователей поблизости с учетом фильтров
      List<User> nearbyUsers = userService.findNearbyUsers(chatId, latitude, longitude, radius);
      
      // Кэшируем результаты поиска
      stateManager.cacheNearbyUsers(chatId, nearbyUsers);
      
      // Отображаем информацию о найденных пользователях
      showNearbyUsers(chatId, nearbyUsers);
    } else {
      messageSender.sendTextMessage(chatId, "⚠️ Пожалуйста, выберите время и радиус перед отправкой геолокации.");
    }
  }
  
  /**
   * Отображает информацию о найденных пользователях
   */
  private void showNearbyUsers(Long chatId, List<User> nearbyUsers) {
    if (nearbyUsers == null || nearbyUsers.isEmpty()) {
      messageSender.sendTextMessage(chatId,
          "😔 На данный момент никого поблизости не найдено, попробуйте позже.\n\n" +
              "📍 У вас активна геолокация на " + stateManager.getLocationDuration(chatId) +
              " часов. Если кто-то окажется рядом, мы вам сообщим!");
      return;
    }
    
    // Показываем информацию о количестве найденных пользователей
    messageSender.sendTextMessage(chatId, "🔍 Найдено " + nearbyUsers.size() + " человек поблизости!");
    
    // Отобразим первого пользователя
    if (callbackQueryHandler != null) {
      callbackQueryHandler.showCurrentNearbyUser(chatId);
    } else {
      messageSender.sendTextMessage(chatId, 
          "⚠️ Не удалось отобразить профиль пользователя. Пожалуйста, обновите геолокацию позже.");
    }
  }
}