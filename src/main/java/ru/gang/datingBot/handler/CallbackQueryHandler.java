package ru.gang.datingBot.handler;

import java.util.List;
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
    // Обработка команд, связанных с профилем
    if (data.startsWith("edit_profile_")) {
      String field = data.replace("edit_profile_", "");
      processProfileEdit(chatId, field, messageId);
      return;
    }

    if (data.startsWith("send_request_")) {
      Long receiverId = Long.parseLong(data.replace("send_request_", ""));
      stateManager.saveMeetingRequestTarget(chatId, receiverId);

      System.out.println("DEBUG: Начинаем отправку запроса на встречу от " + chatId + " к " + receiverId);
      messageSender.deleteMessage(chatId, messageId);
      messageSender.sendTextMessage(chatId, "📝 Напишите сообщение для запроса на встречу:");
      stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_MEETING_MESSAGE);
    }

    // Обработка выбора пола
    if (data.startsWith("gender_")) {
      if (!data.startsWith("gender_pref_")) { // проверяем, что это не предпочтения пола
        String gender = data.replace("gender_", "");
        userService.updateUserGender(chatId, gender);

        messageSender.deleteMessage(chatId, messageId);
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

      messageSender.deleteMessage(chatId, messageId);
      messageSender.sendTextMessage(chatId, "✅ Настройки поиска обновлены!\n\n" +
              "🔍 Возраст: " + user.getMinAgePreference() + " - " + user.getMaxAgePreference() + " лет\n" +
              "👥 Пол: " + profileService.getGenderPreferenceDisplay(genderPref));

      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }

    // Обработка выбора времени
    if (data.equals("1 час") || data.equals("3 часа") || data.equals("6 часов")) {
      int duration = Integer.parseInt(data.split(" ")[0]);
      stateManager.saveLocationDuration(chatId, duration);

      // Удаляем предыдущее сообщение с кнопками
      messageSender.deleteMessage(chatId, messageId);
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
      messageSender.deleteMessage(chatId, messageId);
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

      messageSender.deleteMessage(chatId, messageId);
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

    // Принятие запроса на встречу
    if (data.startsWith("accept_request_")) {
      Long senderId = Long.parseLong(data.replace("accept_request_", ""));
      Long receiverId = chatId;

      // Находим запрос
      List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
      for (MeetingRequest request : requests) {
        if (request.getSender().getTelegramId().equals(senderId)) {
          meetingService.acceptMeetingRequest(request.getId());
          // Уведомляем отправителя о принятии запроса
          messageSender.sendTextMessage(senderId, "✅ Ваш запрос на встречу был принят!");
          messageSender.sendTextMessage(chatId, "Вы приняли запрос на встречу!");
          break;
        }
      }
    }

    // Отклонение запроса на встречу
    if (data.startsWith("decline_request_")) {
      Long senderId = Long.parseLong(data.replace("decline_request_", ""));
      Long receiverId = chatId;

      // Находим запрос
      List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
      for (MeetingRequest request : requests) {
        if (request.getSender().getTelegramId().equals(senderId)) {
          meetingService.declineMeetingRequest(request.getId());
          // Уведомляем отправителя об отклонении запроса
          messageSender.sendTextMessage(senderId, "❌ Ваш запрос на встречу был отклонен.");
          messageSender.sendTextMessage(chatId, "Вы отклонили запрос на встречу.");
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
    messageSender.deleteMessage(chatId, messageId);

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

    messageSender.sendMarkdownMessage(chatId, profileService.formatSearchSettings(user));

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
    messageSender.deleteMessage(chatId, messageId);

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
    messageSender.deleteMessage(chatId, messageId);

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
      messageSender.sendPhoto(chatId, profile.getPhotoFileId(), null);
    }
  }
}