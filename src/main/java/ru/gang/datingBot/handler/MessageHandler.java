package ru.gang.datingBot.handler;

import java.time.LocalDateTime;
import ru.gang.datingBot.service.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.service.ProfileService;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.UserService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageHandler {

  private final UserService userService;
  private final MeetingService meetingService;
  private final UserStateManager stateManager;
  private final KeyboardService keyboardService;
  private final ProfileService profileService;
  private final MessageSender messageSender;

  public void processTextMessage(Long chatId, String text) {
    UserStateManager.UserState currentState = stateManager.getUserState(chatId);

    switch (currentState) {
      case WAITING_FOR_DESCRIPTION:
        userService.updateUserDescription(chatId, text);
        messageSender.sendTextMessage(chatId, "✅ Ваше описание обновлено! Теперь расскажите о своих интересах и хобби.");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_INTERESTS);
        return;

      case WAITING_FOR_INTERESTS:
        userService.updateUserInterests(chatId, text);
        messageSender.sendTextMessage(chatId, "✅ Ваши интересы обновлены! Теперь укажите ваш возраст.");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_AGE);
        return;

      case WAITING_FOR_AGE:
        try {
          int age = Integer.parseInt(text.trim());
          if (age < 18 || age > 100) {
            messageSender.sendTextMessage(chatId, "⚠️ Пожалуйста, введите корректный возраст (от 18 до 100 лет).");
            return;
          }
          userService.updateUserAge(chatId, age);
          messageSender.sendTextMessageWithKeyboard(
                  chatId,
                  "Выберите ваш пол:",
                  keyboardService.createGenderSelectionKeyboard());
          stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_GENDER);
        } catch (NumberFormatException e) {
          messageSender.sendTextMessage(chatId, "⚠️ Пожалуйста, введите возраст числом.");
        }
        return;

      case WAITING_FOR_MIN_AGE:
        try {
          int minAge = Integer.parseInt(text.trim());
          if (minAge < 18 || minAge > 100) {
            messageSender.sendTextMessage(chatId, "⚠️ Пожалуйста, введите корректный минимальный возраст (от 18 до 100 лет).");
            return;
          }

          User user = userService.getUserByTelegramId(chatId);
          userService.updateUserSearchPreferences(chatId, minAge, user.getMaxAgePreference(), user.getGenderPreference());

          messageSender.sendTextMessage(chatId, "✅ Минимальный возраст установлен на " + minAge + " лет. Теперь укажите максимальный возраст для поиска.");
          stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_MAX_AGE);
        } catch (NumberFormatException e) {
          messageSender.sendTextMessage(chatId, "⚠️ Пожалуйста, введите возраст числом.");
        }
        return;

      case WAITING_FOR_MAX_AGE:
        try {
          int maxAge = Integer.parseInt(text.trim());
          if (maxAge < 18 || maxAge > 100) {
            messageSender.sendTextMessage(chatId, "⚠️ Пожалуйста, введите корректный максимальный возраст (от 18 до 100 лет).");
            return;
          }

          User user = userService.getUserByTelegramId(chatId);
          if (user.getMinAgePreference() != null && maxAge < user.getMinAgePreference()) {
            messageSender.sendTextMessage(chatId, "⚠️ Максимальный возраст должен быть больше или равен минимальному возрасту " + user.getMinAgePreference() + " лет.");
            return;
          }

          userService.updateUserSearchPreferences(chatId, user.getMinAgePreference(), maxAge, user.getGenderPreference());

          messageSender.sendTextMessageWithKeyboard(
                  chatId,
                  "Выберите предпочитаемый пол для поиска:",
                  keyboardService.createGenderPreferenceKeyboard());
          stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_GENDER_PREFERENCE);
        } catch (NumberFormatException e) {
          messageSender.sendTextMessage(chatId, "⚠️ Пожалуйста, введите возраст числом.");
        }
        return;

      case WAITING_FOR_PHOTO:
        messageSender.sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию для вашего профиля.");
        return;

      case WAITING_FOR_MEETING_MESSAGE:
        System.out.println("DEBUG: Сохраняем сообщение для запроса на встречу: " + text);
        stateManager.saveMeetingRequestMessage(chatId, text);
        messageSender.sendTextMessage(chatId, "✅ Сообщение сохранено! Хотите добавить фото к запросу? (отправьте фото или напишите \"нет\")");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_MEETING_PHOTO);
        return;

      case WAITING_FOR_MEETING_PHOTO:
        if (text.equalsIgnoreCase("нет") || text.equalsIgnoreCase("no")) {
          Long targetUserId = stateManager.getMeetingRequestTarget(chatId);
          String message = stateManager.getMeetingRequestMessage(chatId);

          if (targetUserId != null && message != null) {
            System.out.println("DEBUG: Отправляем запрос на встречу от " + chatId + " к " + targetUserId);
            meetingService.sendMeetingRequest(chatId, targetUserId, message, LocalDateTime.now().plusHours(1));

            notifyUserAboutMeetingRequest(targetUserId, chatId);

            messageSender.sendTextMessageWithKeyboard(
                    chatId,
                    "✅ Запрос на встречу отправлен!",
                    keyboardService.createMainKeyboard());

            stateManager.clearMeetingRequestData(chatId);
          } else {
            System.out.println("DEBUG: Ошибка отправки запроса, targetUserId: " + targetUserId + ", message: " + message);
            messageSender.sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
          }

          stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
        } else {
          messageSender.sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию или напишите \"нет\", если не хотите добавлять фото.");
        }
        return;

      default:
        break;
    }

    switch (text) {
      case "/start":
        messageSender.sendTextMessage(chatId,
                "👋 Добро пожаловать в GeoGreet!\n\n" +
                        "Это бот знакомств с динамической геолокацией. " +
                        "Здесь вы можете находить людей поблизости и знакомиться с ними.");

        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "Выберите, на сколько часов включить геолокацию:",
                keyboardService.createTimeSelectionKeyboard());
        break;

      case "/profile":
      case "👤 Мой профиль":
        showUserProfile(chatId);
        break;

      case "/edit_profile":
        startProfileEditing(chatId);
        break;

      case "/search_settings":
        showSearchSettings(chatId);
        break;

      case "🔄 Обновить геолокацию":
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "Выберите, на сколько часов включить геолокацию:",
                keyboardService.createTimeSelectionKeyboard());
        break;

      case "❌ Остановить поиск":
        userService.deactivateUser(chatId);
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "Вы больше не видимы для других пользователей.",
                keyboardService.createMainKeyboard());
        break;
    }
  }

  private void showUserProfile(Long chatId) {
    User user = userService.getUserByTelegramId(chatId);

    if (user == null) {
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "⚠️ Профиль не найден. Используйте /edit_profile, чтобы создать свой профиль.",
              keyboardService.createMainKeyboard());
      return;
    }

    if (user.getPhotoFileId() != null && !user.getPhotoFileId().isEmpty()) {
      try {
        messageSender.sendPhotoWithMarkdown(chatId, user.getPhotoFileId(), escapeMarkdown(user.getProfileInfo()));
      } catch (Exception e) {
        messageSender.sendTextMessage(chatId, "Информация о вашем профиле:\n\n" + user.getProfileInfo());
      }
    } else {
      messageSender.sendTextMessage(
              chatId,
              user.getProfileInfo() + "\n🔄 Используйте /edit_profile для редактирования профиля.");
    }

    int completionPercentage = user.getProfileCompletionPercentage();
    messageSender.sendTextMessage(chatId, "🏆 Ваш профиль заполнен на " + completionPercentage + "%");

    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Что вы хотите изменить в своем профиле?",
            keyboardService.createProfileEditKeyboard());

    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Вернуться к основным действиям:",
            keyboardService.createMainKeyboard());
  }

  private void startProfileEditing(Long chatId) {
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Выберите, что вы хотите изменить в своем профиле:",
            keyboardService.createProfileEditKeyboard());
  }

  private void showSearchSettings(Long chatId) {
    User user = userService.getUserByTelegramId(chatId);

    if (user == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Профиль не найден. Используйте /edit_profile, чтобы создать свой профиль.");
      return;
    }

    try {
      messageSender.sendTextMessage(chatId, escapeMarkdown(profileService.formatSearchSettings(user)));
    } catch (Exception e) {
      messageSender.sendTextMessage(chatId, "Ваши настройки поиска:\n\n" + profileService.formatSearchSettings(user));
    }

    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Что вы хотите изменить?",
            keyboardService.createSearchSettingsKeyboard());
  }

  private String escapeMarkdown(String text) {
    if (text == null) return "";
    return text
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("`", "\\`");
  }

  private void notifyUserAboutMeetingRequest(Long receiverId, Long senderId) {
    System.out.println("DEBUG: Начало отправки уведомления о запросе на встречу от " + senderId + " к " + receiverId);
    User sender = userService.getUserByTelegramId(senderId);
    String message = stateManager.getMeetingRequestMessage(senderId);

    if (sender == null || message == null) {
      System.out.println("DEBUG: Ошибка - отправитель или сообщение не найдены");
      return;
    }

    System.out.println("DEBUG: Форматирование информации о запросе");
    
    String requestInfo = profileService.formatMeetingRequest(sender, message);

    System.out.println("DEBUG: Отправка сообщения с кнопками принятия/отклонения");
    try {
      messageSender.sendTextMessageWithKeyboard(
              receiverId,
              requestInfo,
              keyboardService.createMeetingRequestKeyboard(senderId));
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке уведомления с кнопками: " + e.getMessage());
      messageSender.sendTextMessage(
              receiverId,
              requestInfo + "\n\nЧтобы ответить, используйте команды:\n/accept_" + senderId + " - принять\n/decline_" + senderId + " - отклонить");
    }

    if (sender.getPhotoFileId() != null && !sender.getPhotoFileId().isEmpty()) {
      System.out.println("DEBUG: Отправка фото профиля отправителя");
      try {
        messageSender.sendPhoto(receiverId, sender.getPhotoFileId(), null);
      } catch (Exception e) {
        System.out.println("DEBUG: Ошибка при отправке фото профиля: " + e.getMessage());
      }
    }

    String photoFileId = stateManager.getMeetingRequestPhoto(senderId);
    if (photoFileId != null && !photoFileId.isEmpty()) {
      System.out.println("DEBUG: Отправка фото из запроса");
      try {
        messageSender.sendPhoto(receiverId, photoFileId, "📸 Фото к запросу на встречу");
      } catch (Exception e) {
        System.out.println("DEBUG: Ошибка при отправке фото запроса: " + e.getMessage());
      }
    }
    
    System.out.println("DEBUG: Уведомление о запросе на встречу отправлено");
  }
}