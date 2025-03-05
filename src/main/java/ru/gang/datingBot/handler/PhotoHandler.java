package ru.gang.datingBot.handler;

import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.gang.datingBot.bot.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.ProfileService;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.bot.UserStateManager.UserState;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.UserService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Обработчик сообщений с фотографиями
 */
public class PhotoHandler {

  private final UserService userService;
  private final MeetingService meetingService;
  private final UserStateManager stateManager;
  private final MessageSender messageSender;

  public PhotoHandler(
          UserService userService,
          MeetingService meetingService,
          UserStateManager stateManager,
          MessageSender messageSender) {
    this.userService = userService;
    this.meetingService = meetingService;
    this.stateManager = stateManager;
    this.messageSender = messageSender;
  }

  /**
   * Обработка загрузки фотографий
   */
  public void processPhotoMessage(Long chatId, List<PhotoSize> photos, Integer messageId) {
    UserState currentState = stateManager.getUserState(chatId);

    // Получаем самое большое фото (лучшее качество)
    PhotoSize largestPhoto = photos.stream()
            .max(Comparator.comparing(PhotoSize::getFileSize))
            .orElse(null);

    if (largestPhoto == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Не удалось обработать фото. Пожалуйста, попробуйте еще раз.");
      return;
    }

    String fileId = largestPhoto.getFileId();

    switch (currentState) {
      case WAITING_FOR_PHOTO:
        processProfilePhoto(chatId, fileId);
        break;

      case WAITING_FOR_MEETING_PHOTO:
        processMeetingPhoto(chatId, fileId);
        break;

      default:
        // Пользователь отправил фото вне контекста создания профиля
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "📸 Хотите обновить фото профиля? Используйте команду /edit_profile",
                new KeyboardService().createMainKeyboard());
        break;
    }
  }

  /**
   * Обработка фото профиля
   */
  private void processProfilePhoto(Long chatId, String fileId) {
    // Сохраняем фото профиля
    userService.updateUserPhoto(chatId, fileId);

    int completionPercentage = userService.getProfileCompletionPercentage(chatId);
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "✅ Ваше фото профиля обновлено!\n\n" +
                    "🏆 Ваш профиль заполнен на " + completionPercentage + "%\n\n" +
                    "Чтобы просмотреть свой профиль, используйте команду /profile\n" +
                    "Для редактирования профиля используйте /edit_profile",
            new KeyboardService().createMainKeyboard());

    stateManager.setUserState(chatId, UserState.NONE);
  }

  /**
   * Обработка фото для запроса на встречу
   */
  private void processMeetingPhoto(Long chatId, String fileId) {
    // Сохраняем фото для запроса на встречу
    stateManager.saveMeetingRequestPhoto(chatId, fileId);

    Long targetUserId = stateManager.getMeetingRequestTarget(chatId);
    String message = stateManager.getMeetingRequestMessage(chatId);

    if (targetUserId != null && message != null) {
      meetingService.sendMeetingRequest(chatId, targetUserId, message, LocalDateTime.now().plusHours(1), fileId);

      // Уведомляем получателя о запросе
      notifyUserAboutMeetingRequest(targetUserId, chatId);

      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "✅ Запрос на встречу с фото отправлен!",
              new KeyboardService().createMainKeyboard());

      // Очищаем временные данные
      stateManager.clearMeetingRequestData(chatId);
    } else {
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
    }

    stateManager.setUserState(chatId, UserState.NONE);
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

    ProfileService profileService = new ProfileService(userService, new KeyboardService());
    String requestInfo = profileService.formatMeetingRequest(sender, message);

    messageSender.sendTextMessageWithKeyboard(
            receiverId,
            requestInfo,
            new KeyboardService().createMeetingRequestKeyboard(senderId));

    if (sender.getPhotoFileId() != null && !sender.getPhotoFileId().isEmpty()) {
      messageSender.sendPhoto(receiverId, sender.getPhotoFileId(), null);
    }

    String photoFileId = stateManager.getMeetingRequestPhoto(senderId);
    if (photoFileId != null && !photoFileId.isEmpty()) {
      messageSender.sendPhoto(receiverId, photoFileId, "📸 Фото к запросу на встречу");
    }
  }
}