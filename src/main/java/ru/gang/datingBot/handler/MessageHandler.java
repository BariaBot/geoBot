package ru.gang.datingBot.handler;

import java.time.LocalDateTime;
import ru.gang.datingBot.bot.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.ProfileService;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.UserService;

/**
 * Обработчик текстовых сообщений от пользователей
 */
public class MessageHandler {

  private final UserService userService;
  private final MeetingService meetingService;
  private final UserStateManager stateManager;
  private final KeyboardService keyboardService;
  private final ProfileService profileService;
  private final MessageSender messageSender;

  public MessageHandler(
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
   * Обрабатывает текстовые сообщения от пользователей
   */
  public void processTextMessage(Long chatId, String text) {
    // Проверяем состояние пользователя в первую очередь для обработки процесса создания профиля
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
          // Временно сохраняем только минимальный возраст
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

          // Теперь сохраняем оба значения возраста
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
        // Сохраняем сообщение для запроса на встречу
        System.out.println("DEBUG: Сохраняем сообщение для запроса на встречу: " + text);
        stateManager.saveMeetingRequestMessage(chatId, text);
        messageSender.sendTextMessage(chatId, "✅ Сообщение сохранено! Хотите добавить фото к запросу? (отправьте фото или напишите \"нет\")");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_MEETING_PHOTO);
        return;

      case WAITING_FOR_MEETING_PHOTO:
        if (text.equalsIgnoreCase("нет") || text.equalsIgnoreCase("no")) {
          // Отправляем запрос без фото
          Long targetUserId = stateManager.getMeetingRequestTarget(chatId);
          String message = stateManager.getMeetingRequestMessage(chatId);

          if (targetUserId != null && message != null) {
            meetingService.sendMeetingRequest(chatId, targetUserId, message, LocalDateTime.now().plusHours(1));

            // Уведомляем получателя о запросе
            notifyUserAboutMeetingRequest(targetUserId, chatId);

            messageSender.sendTextMessageWithKeyboard(
                    chatId,
                    "✅ Запрос на встречу отправлен!",
                    keyboardService.createMainKeyboard());

            // Очищаем временные данные
            stateManager.clearMeetingRequestData(chatId);
          } else {
            messageSender.sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
          }

          stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
        } else {
          messageSender.sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию или напишите \"нет\", если не хотите добавлять фото.");
        }
        return;

      default:
        // Обработка команд или обычных сообщений
        break;
    }

    // Обработка команд
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

  /**
   * Показывает профиль пользователя
   */
  private void showUserProfile(Long chatId) {
    User user = userService.getUserByTelegramId(chatId);

    if (user == null) {
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "⚠️ Профиль не найден. Используйте /edit_profile, чтобы создать свой профиль.",
              keyboardService.createMainKeyboard());
      return;
    }

    // Если у пользователя есть фото профиля, отправляем его с информацией профиля
    if (user.getPhotoFileId() != null && !user.getPhotoFileId().isEmpty()) {
      messageSender.sendPhotoWithMarkdown(chatId, user.getPhotoFileId(), user.getProfileInfo());
    } else {
      // Отправляем профиль только с текстом
      messageSender.sendMarkdownMessage(
              chatId,
              user.getProfileInfo() + "\n🔄 Используйте /edit_profile для редактирования профиля.");
    }

    // Показываем процент заполненности
    int completionPercentage = user.getProfileCompletionPercentage();
    messageSender.sendTextMessage(chatId, "🏆 Ваш профиль заполнен на " + completionPercentage + "%");

    // Показываем кнопки редактирования, а затем основную клавиатуру
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Что вы хотите изменить в своем профиле?",
            keyboardService.createProfileEditKeyboard());

    // После инлайн-клавиатуры для редактирования показываем основную клавиатуру
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Вернуться к основным действиям:",
            keyboardService.createMainKeyboard());
  }

  /**
   * Начинаем процесс редактирования профиля
   */
  private void startProfileEditing(Long chatId) {
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Выберите, что вы хотите изменить в своем профиле:",
            keyboardService.createProfileEditKeyboard());
  }

  /**
   * Отображает настройки поиска
   */
  private void showSearchSettings(Long chatId) {
    User user = userService.getUserByTelegramId(chatId);

    if (user == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Профиль не найден. Используйте /edit_profile, чтобы создать свой профиль.");
      return;
    }

    // Отправляем информацию о текущих настройках
    messageSender.sendMarkdownMessage(chatId, profileService.formatSearchSettings(user));

    // Показываем кнопки для изменения настроек
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Что вы хотите изменить?",
            keyboardService.createSearchSettingsKeyboard());
  }

  /**
   * Уведомляет пользователя о запросе на встречу
   */
  private void notifyUserAboutMeetingRequest(Long receiverId, Long senderId) {
    User sender = userService.getUserByTelegramId(senderId);
    String message = stateManager.getMeetingRequestMessage(senderId);

    if (sender == null || message == null) {
      return;
    }

    // Форматируем сообщение о запросе на встречу
    String requestInfo = profileService.formatMeetingRequest(sender, message);

    // Отправляем сообщение с кнопками принятия/отклонения
    messageSender.sendTextMessageWithKeyboard(
            receiverId,
            requestInfo,
            keyboardService.createMeetingRequestKeyboard(senderId));

    // Если у отправителя есть фото профиля, отправляем его отдельно
    if (sender.getPhotoFileId() != null && !sender.getPhotoFileId().isEmpty()) {
      messageSender.sendPhoto(receiverId, sender.getPhotoFileId(), null);
    }

    // Если в запросе есть фото, отправляем его отдельно
    String photoFileId = stateManager.getMeetingRequestPhoto(senderId);
    if (photoFileId != null && !photoFileId.isEmpty()) {
      messageSender.sendPhoto(receiverId, photoFileId, "📸 Фото к запросу на встречу");
    }
  }
}