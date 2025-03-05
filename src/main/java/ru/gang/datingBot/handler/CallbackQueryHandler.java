package ru.gang.datingBot.handler;

import java.util.List;
import lombok.Setter;
import ru.gang.datingBot.bot.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.ProfileService;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.UserService;

/**
 * Обработчик для callback-запросов от встроенных кнопок
 */
public class CallbackQueryHandler {

  private final UserService userService;
  private final MeetingService meetingService;
  private final UserStateManager stateManager;
  private final KeyboardService keyboardService;
  private final ProfileService profileService;
  private final MessageSender messageSender;
  
  @Setter
  private ChatHandler chatHandler; // Не передаем в конструкторе, устанавливаем через сеттер
  
  @Setter
  private MeetingPlaceHandler meetingPlaceHandler;

  public CallbackQueryHandler(
          UserService userService,
          MeetingService meetingService,
          UserStateManager stateManager,
          KeyboardService keyboardService,
          ProfileService profileService,
          MessageSender messageSender) {
    this.userService = userService;
    this.meetingService = meetingService;
    this.stateManager = stateManager;
    this.keyboardService = keyboardService;
    this.profileService = profileService;
    this.messageSender = messageSender;
  }

  /**
   * Обрабатывает callback-запросы от встроенных кнопок
   */
  public void processCallbackQuery(Long chatId, String data, Integer messageId) {
    System.out.println("DEBUG: Получен callback: " + data + " от пользователя " + chatId);
    
    // Обработка команд, связанных с профилем
    if (data.startsWith("edit_profile_")) {
      String field = data.replace("edit_profile_", "");
      processProfileEdit(chatId, field, messageId);
      return;
    }

    // Обработка выбора пола
    if (data.startsWith("gender_")) {
      if (!data.startsWith("gender_pref_")) { // проверяем, что это не предпочтения пола
        String gender = data.replace("gender_", "");
        userService.updateUserGender(chatId, gender);

        try {
          messageSender.deleteMessage(chatId, messageId);
        } catch (Exception e) {
          System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
        }
        
        messageSender.sendTextMessage(chatId, "✅ Ваш пол установлен: " + profileService.getGenderDisplay(gender));

        messageSender.sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию для вашего профиля:");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_PHOTO);
        return;
      }
    }

    // Обработка выбора предпочтений по полу
    if (data.startsWith("gender_pref_")) {
      String genderPref = data.replace("gender_pref_", "");

      User user = userService.getUserByTelegramId(chatId);
      userService.updateUserSearchPreferences(chatId, user.getMinAgePreference(), user.getMaxAgePreference(), genderPref);

      try {
        messageSender.deleteMessage(chatId, messageId);
      } catch (Exception e) {
        System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
      }
      
      messageSender.sendTextMessage(chatId, "✅ Настройки поиска обновлены!\n\n" +
              "🔍 Возраст: " + (user.getMinAgePreference() != null ? user.getMinAgePreference() : "любой") + 
              " - " + (user.getMaxAgePreference() != null ? user.getMaxAgePreference() : "любой") + " лет\n" +
              "👥 Пол: " + profileService.getGenderPreferenceDisplay(genderPref));

      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }

    // Обработка выбора времени
    if (data.equals("1 час") || data.equals("3 часа") || data.equals("6 часов")) {
      int duration = Integer.parseInt(data.split(" ")[0]);
      stateManager.saveLocationDuration(chatId, duration);

      // Удаляем предыдущее сообщение с кнопками
      try {
        messageSender.deleteMessage(chatId, messageId);
      } catch (Exception e) {
        System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
      }
      
      messageSender.sendTextMessage(chatId, "✅ Вы запустили поиск людей рядом на " + duration + " часов.");

      // Отправляем выбор радиуса
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "Выберите радиус поиска:",
              keyboardService.createRadiusSelectionKeyboard());
    }

    // Обработка выбора радиуса
    if (data.equals("1 км") || data.equals("3 км") || data.equals("5 км") || data.equals("1500 км")) {
      int radius = Integer.parseInt(data.split(" ")[0]);
      stateManager.saveSearchRadius(chatId, radius);

      // Удаляем предыдущее сообщение с кнопками
      try {
        messageSender.deleteMessage(chatId, messageId);
      } catch (Exception e) {
        System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
      }
      
      messageSender.sendTextMessage(chatId, "📍 Вы выбрали радиус поиска " + radius + " км.");

      // Просим отправить геолокацию
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "Отправьте свою геолокацию, чтобы вас могли найти:",
              keyboardService.createLocationRequestKeyboard());
    }

    // Отправка запроса на встречу
    if (data.startsWith("send_request_")) {
      Long receiverId = Long.parseLong(data.replace("send_request_", ""));
      stateManager.saveMeetingRequestTarget(chatId, receiverId);

      System.out.println("DEBUG: Начинаем отправку запроса на встречу от " + chatId + " к " + receiverId);
      
      try {
        messageSender.deleteMessage(chatId, messageId);
      } catch (Exception e) {
        System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
      }
      
      messageSender.sendTextMessage(chatId, "📝 Напишите сообщение для запроса на встречу:");
      stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_MEETING_MESSAGE);
    }

    // Навигация по списку пользователей
    if (data.equals("next_user")) {
      showNextUser(chatId, messageId);
      return;
    }

    if (data.equals("prev_user")) {
      showPreviousUser(chatId, messageId);
      return;
    }
    
    // Навигация по местам
    if (data.equals("next_place")) {
      meetingPlaceHandler.showNextPlace(chatId);
      return;
    }

    if (data.equals("prev_place")) {
      meetingPlaceHandler.showPreviousPlace(chatId);
      return;
    }

    // Выбор места
    if (data.startsWith("select_place_")) {
      Long placeId = Long.parseLong(data.replace("select_place_", ""));
      meetingPlaceHandler.processPlaceSelection(chatId, placeId);
      return;
    }

    // Выбор даты
    if (data.startsWith("date_")) {
      String dateString = data.replace("date_", "");
      meetingPlaceHandler.processDateSelection(chatId, dateString);
      return;
    }

    // Выбор времени
    if (data.startsWith("time_")) {
      String timeString = data.replace("time_", "");
      meetingPlaceHandler.processTimeSelection(chatId, timeString);
      return;
    }

    // Подтверждение встречи
    if (data.startsWith("confirm_meeting_")) {
      Long meetingRequestId = Long.parseLong(data.replace("confirm_meeting_", ""));
      meetingPlaceHandler.processConfirmation(chatId, meetingRequestId);
      return;
    }

    // Оценка встречи
    if (data.startsWith("rate_meeting_")) {
      String[] parts = data.replace("rate_meeting_", "").split("_");
      if (parts.length == 2) {
        Long meetingRequestId = Long.parseLong(parts[0]);
        int rating = Integer.parseInt(parts[1]);
        // Здесь можно добавить обработку оценки встречи
        messageSender.sendTextMessage(chatId, "Спасибо за вашу оценку! Ваш отзыв очень важен для нас.");
      }
      return;
    }

    // Принятие запроса на встречу
    if (data.startsWith("accept_request_")) {
      Long senderId = Long.parseLong(data.replace("accept_request_", ""));
      Long receiverId = chatId;

      System.out.println("DEBUG: Принятие запроса на встречу от " + senderId + " пользователем " + receiverId);
      
      // Находим запрос
      List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
      for (MeetingRequest request : requests) {
        if (request.getSender().getTelegramId().equals(senderId)) {
          try {
            meetingService.acceptMeetingRequest(request.getId());
            
            // Инициализируем чат между пользователями если ChatHandler доступен
            if (chatHandler != null) {
              chatHandler.initializeChat(senderId, receiverId, request.getId());
            } else {
              // Простое уведомление в случае, если ChatHandler недоступен
              messageSender.sendTextMessage(senderId, "✅ Ваш запрос на встречу был принят!");
              messageSender.sendTextMessage(chatId, "Вы приняли запрос на встречу!");
            }
            
            System.out.println("DEBUG: Запрос на встречу от " + senderId + " принят пользователем " + receiverId);
          } catch (Exception e) {
            System.out.println("DEBUG: Ошибка при принятии запроса: " + e.getMessage());
            messageSender.sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
          }
          break;
        }
      }
    }

    // Отклонение запроса на встречу
    if (data.startsWith("decline_request_")) {
      Long senderId = Long.parseLong(data.replace("decline_request_", ""));
      Long receiverId = chatId;

      System.out.println("DEBUG: Отклонение запроса на встречу от " + senderId + " пользователем " + receiverId);
      
      // Находим запрос
      List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
      for (MeetingRequest request : requests) {
        if (request.getSender().getTelegramId().equals(senderId)) {
          try {
            meetingService.declineMeetingRequest(request.getId());
            // Уведомляем отправителя об отклонении запроса
            messageSender.sendTextMessage(senderId, "❌ Ваш запрос на встречу был отклонен.");
            messageSender.sendTextMessage(chatId, "Вы отклонили запрос на встречу.");
            System.out.println("DEBUG: Запрос на встречу от " + senderId + " отклонен пользователем " + receiverId);
          } catch (Exception e) {
            System.out.println("DEBUG: Ошибка при отклонении запроса: " + e.getMessage());
            messageSender.sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
          }
          break;
        }
      }
    }
  }

  /**
   * Обработка выбора редактирования профиля
   */
  private void processProfileEdit(Long chatId, String field, Integer messageId) {
    // Удаляем предыдущее сообщение с кнопками
    try {
      messageSender.deleteMessage(chatId, messageId);
    } catch (Exception e) {
      System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
    }

    switch (field) {
      case "description":
        messageSender.sendTextMessage(chatId, "📝 Пожалуйста, напишите короткое описание о себе (до 1000 символов):");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_DESCRIPTION);
        break;

      case "interests":
        messageSender.sendTextMessage(chatId, "⭐ Пожалуйста, напишите о своих интересах и хобби (до 500 символов):");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_INTERESTS);
        break;

      case "age":
        messageSender.sendTextMessage(chatId, "🎂 Пожалуйста, введите ваш возраст (число от 18 до 100):");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_AGE);
        break;

      case "gender":
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "Выберите ваш пол:",
                keyboardService.createGenderSelectionKeyboard());
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_GENDER);
        break;

      case "photo":
        messageSender.sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию для вашего профиля:");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_PHOTO);
        break;

      case "search":
        showSearchSettings(chatId);
        break;

      case "age_range":
        messageSender.sendTextMessage(chatId, "🎯 Пожалуйста, введите минимальный возраст для поиска (число от 18 до 100):");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_MIN_AGE);
        break;

      case "gender_pref":
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "Выберите предпочитаемый пол для поиска:",
                keyboardService.createGenderPreferenceKeyboard());
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_GENDER_PREFERENCE);
        break;
    }
  }

  /**
   * Показывает настройки поиска
   */
  private void showSearchSettings(Long chatId) {
    User user = userService.getUserByTelegramId(chatId);

    if (user == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Профиль не найден. Используйте /edit_profile, чтобы создать свой профиль.");
      return;
    }

    try {
      messageSender.sendTextMessage(chatId, profileService.formatSearchSettings(user));
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отображении настроек поиска: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "Ваши настройки поиска:\n" +
              "🎯 Возраст: " + (user.getMinAgePreference() != null ? user.getMinAgePreference() : "любой") + 
              " - " + (user.getMaxAgePreference() != null ? user.getMaxAgePreference() : "любой") + " лет\n" +
              "👥 Пол: " + user.getGenderPreferenceDisplay() + "\n" +
              "📍 Радиус поиска: " + user.getSearchRadius() + " км");
    }

    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Что вы хотите изменить?",
            keyboardService.createSearchSettingsKeyboard());
  }

  /**
   * Показывает следующего пользователя из списка найденных
   */
  private void showNextUser(Long chatId, Integer messageId) {
    // Удаляем предыдущее сообщение
    try {
      messageSender.deleteMessage(chatId, messageId);
    } catch (Exception e) {
      System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
    }

    List<User> nearbyUsers = stateManager.getNearbyUsersCache(chatId);
    Integer currentIndex = stateManager.getCurrentUserIndex(chatId);

    if (nearbyUsers == null || nearbyUsers.isEmpty() || currentIndex == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Список пользователей не найден. Пожалуйста, обновите геолокацию.");
      return;
    }

    // Переходим к следующему пользователю или возвращаемся к началу
    currentIndex = (currentIndex + 1) % nearbyUsers.size();
    stateManager.setCurrentUserIndex(chatId, currentIndex);

    // Показываем нового пользователя
    showCurrentNearbyUser(chatId);
  }

  /**
   * Показывает предыдущего пользователя из списка найденных
   */
  private void showPreviousUser(Long chatId, Integer messageId) {
    // Удаляем предыдущее сообщение
    try {
      messageSender.deleteMessage(chatId, messageId);
    } catch (Exception e) {
      System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
    }

    List<User> nearbyUsers = stateManager.getNearbyUsersCache(chatId);
    Integer currentIndex = stateManager.getCurrentUserIndex(chatId);

    if (nearbyUsers == null || nearbyUsers.isEmpty() || currentIndex == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Список пользователей не найден. Пожалуйста, обновите геолокацию.");
      return;
    }

    // Переходим к предыдущему пользователю или к последнему в списке
    currentIndex = (currentIndex - 1 + nearbyUsers.size()) % nearbyUsers.size();
    stateManager.setCurrentUserIndex(chatId, currentIndex);

    // Показываем нового пользователя
    showCurrentNearbyUser(chatId);
  }

  /**
   * Показывает текущего пользователя из списка найденных
   */
  public void showCurrentNearbyUser(Long chatId) {
    List<User> nearbyUsers = stateManager.getNearbyUsersCache(chatId);
    Integer currentIndex = stateManager.getCurrentUserIndex(chatId);

    if (nearbyUsers == null || nearbyUsers.isEmpty()) {
      messageSender.sendTextMessageWithKeyboard(chatId,
              "😔 На данный момент никого поблизости не найдено, попробуйте позже.\n\n" +
                      "📍 У вас активна геолокация на " + stateManager.getLocationDuration(chatId) +
                      " часов. Если кто-то окажется рядом, мы вам сообщим!",
              keyboardService.createMainKeyboard());
      return;
    }

    if (currentIndex == null || currentIndex < 0 || currentIndex >= nearbyUsers.size()) {
      currentIndex = 0;
      stateManager.setCurrentUserIndex(chatId, currentIndex);
    }

    User profile = nearbyUsers.get(currentIndex);
    System.out.println("DEBUG: Показываем профиль " + profile.getTelegramId() + " для пользователя " + chatId);

    // Получаем информацию о профиле в отформатированном виде
    String profileInfo = profileService.formatNearbyUserProfile(profile, currentIndex, nearbyUsers.size());

    // Отправляем информацию о профиле с кнопками
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            profileInfo,
            keyboardService.createNearbyUserNavigationKeyboard(
                    profile.getTelegramId(),
                    nearbyUsers.size() > 1));

    // Если у пользователя есть фото, отправляем его отдельно
    if (profile.getPhotoFileId() != null && !profile.getPhotoFileId().isEmpty()) {
      try {
        messageSender.sendPhoto(chatId, profile.getPhotoFileId(), null);
      } catch (Exception e) {
        System.out.println("DEBUG: Ошибка при отправке фото профиля: " + e.getMessage());
      }
    }
  }
}
