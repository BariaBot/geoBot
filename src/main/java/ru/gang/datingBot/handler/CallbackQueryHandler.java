package ru.gang.datingBot.handler;

import java.util.List;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import ru.gang.datingBot.service.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.service.ProfileService;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.UserService;

@RequiredArgsConstructor
public class CallbackQueryHandler {

  private final UserService userService;
  private final MeetingService meetingService;
  private final UserStateManager stateManager;
  private final KeyboardService keyboardService;
  private final ProfileService profileService;
  private final MessageSender messageSender;
  
  @Setter
  private ChatHandler chatHandler;

  public void processCallbackQuery(Long chatId, String data, Integer messageId) {
    System.out.println("DEBUG: Получен callback: " + data + " от пользователя " + chatId);
    
    if (data.startsWith("edit_profile_")) {
      String field = data.replace("edit_profile_", "");
      processProfileEdit(chatId, field, messageId);
      return;
    }

    if (data.startsWith("gender_")) {
      if (!data.startsWith("gender_pref_")) {
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

    if (data.equals("1 час") || data.equals("3 часа") || data.equals("6 часов")) {
      int duration = Integer.parseInt(data.split(" ")[0]);
      stateManager.saveLocationDuration(chatId, duration);

      try {
        messageSender.deleteMessage(chatId, messageId);
      } catch (Exception e) {
        System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
      }
      
      messageSender.sendTextMessage(chatId, "✅ Вы запустили поиск людей рядом на " + duration + " часов.");

      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "Выберите радиус поиска:",
              keyboardService.createRadiusSelectionKeyboard());
    }

    if (data.equals("1 км") || data.equals("3 км") || data.equals("5 км") || data.equals("1500 км")) {
      int radius = Integer.parseInt(data.split(" ")[0]);
      stateManager.saveSearchRadius(chatId, radius);

      try {
        messageSender.deleteMessage(chatId, messageId);
      } catch (Exception e) {
        System.out.println("DEBUG: Не удалось удалить сообщение: " + e.getMessage());
      }
      
      messageSender.sendTextMessage(chatId, "📍 Вы выбрали радиус поиска " + radius + " км.");

      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "Отправьте свою геолокацию, чтобы вас могли найти:",
              keyboardService.createLocationRequestKeyboard());
    }

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

    if (data.equals("next_user")) {
      showNextUser(chatId, messageId);
      return;
    }

    if (data.equals("prev_user")) {
      showPreviousUser(chatId, messageId);
      return;
    }

    if (data.startsWith("accept_request_")) {
      Long senderId = Long.parseLong(data.replace("accept_request_", ""));
      Long receiverId = chatId;

      System.out.println("DEBUG: Принятие запроса на встречу от " + senderId + " пользователем " + receiverId);
      
      List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
      for (MeetingRequest request : requests) {
        if (request.getSender().getTelegramId().equals(senderId)) {
          try {
            meetingService.acceptMeetingRequest(request.getId());
            
            if (chatHandler != null) {
              chatHandler.initializeChat(senderId, receiverId, request.getId());
            } else {
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

    if (data.startsWith("decline_request_")) {
      Long senderId = Long.parseLong(data.replace("decline_request_", ""));
      Long receiverId = chatId;

      System.out.println("DEBUG: Отклонение запроса на встречу от " + senderId + " пользователем " + receiverId);
      
      List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
      for (MeetingRequest request : requests) {
        if (request.getSender().getTelegramId().equals(senderId)) {
          try {
            meetingService.declineMeetingRequest(request.getId());
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

  private void processProfileEdit(Long chatId, String field, Integer messageId) {
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

  private void showNextUser(Long chatId, Integer messageId) {
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

    currentIndex = (currentIndex + 1) % nearbyUsers.size();
    stateManager.setCurrentUserIndex(chatId, currentIndex);

    showCurrentNearbyUser(chatId);
  }

  private void showPreviousUser(Long chatId, Integer messageId) {
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

    currentIndex = (currentIndex - 1 + nearbyUsers.size()) % nearbyUsers.size();
    stateManager.setCurrentUserIndex(chatId, currentIndex);

    showCurrentNearbyUser(chatId);
  }

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

    String profileInfo = profileService.formatNearbyUserProfile(profile, currentIndex, nearbyUsers.size());

    messageSender.sendTextMessageWithKeyboard(
            chatId,
            profileInfo,
            keyboardService.createNearbyUserNavigationKeyboard(
                    profile.getTelegramId(),
                    nearbyUsers.size() > 1));

    if (profile.getPhotoFileId() != null && !profile.getPhotoFileId().isEmpty()) {
      try {
        messageSender.sendPhoto(chatId, profile.getPhotoFileId(), null);
      } catch (Exception e) {
        System.out.println("DEBUG: Ошибка при отправке фото профиля: " + e.getMessage());
      }
    }
  }
}