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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhotoHandler {

  private final UserService userService;
  private final MeetingService meetingService;
  private final UserStateManager stateManager;
  private final MessageSender messageSender;

  public void processPhotoMessage(Long chatId, List<PhotoSize> photos, Integer messageId) {
    UserState currentState = stateManager.getUserState(chatId);

    PhotoSize largestPhoto = photos.stream()
            .max(Comparator.comparing(PhotoSize::getFileSize))
            .orElse(null);

    if (largestPhoto == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Не удалось обработать фото. Пожалуйста, попробуйте еще раз.");
      return;
    }

    String fileId = largestPhoto.getFileId();
    System.out.println("DEBUG: Получено фото с fileId: " + fileId);

    switch (currentState) {
      case WAITING_FOR_PHOTO:
        processProfilePhoto(chatId, fileId);
        break;

      case WAITING_FOR_MEETING_PHOTO:
        processMeetingPhoto(chatId, fileId);
        break;

      default:
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "📸 Хотите обновить фото профиля? Используйте команду /edit_profile",
                new KeyboardService().createMainKeyboard());
        break;
    }
  }

  private void processProfilePhoto(Long chatId, String fileId) {
    System.out.println("DEBUG: Обновление фото профиля для пользователя " + chatId);
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

  private void processMeetingPhoto(Long chatId, String fileId) {
    System.out.println("DEBUG: Обработка фото для запроса на встречу от пользователя " + chatId);
    stateManager.saveMeetingRequestPhoto(chatId, fileId);

    Long targetUserId = stateManager.getMeetingRequestTarget(chatId);
    String message = stateManager.getMeetingRequestMessage(chatId);

    if (targetUserId != null && message != null) {
      System.out.println("DEBUG: Отправка запроса на встречу с фото от " + chatId + " к " + targetUserId);
      try {
        meetingService.sendMeetingRequest(chatId, targetUserId, message, LocalDateTime.now().plusHours(1), fileId);

        notifyUserAboutMeetingRequest(targetUserId, chatId);

        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "✅ Запрос на встречу с фото отправлен!",
                new KeyboardService().createMainKeyboard());

        stateManager.clearMeetingRequestData(chatId);
      } catch (Exception e) {
        System.out.println("DEBUG: Ошибка при отправке запроса на встречу: " + e.getMessage());
        messageSender.sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
      }
    } else {
      System.out.println("DEBUG: Ошибка - targetUserId или message равны null");
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
    }

    stateManager.setUserState(chatId, UserState.NONE);
  }

  private void notifyUserAboutMeetingRequest(Long receiverId, Long senderId) {
    System.out.println("DEBUG: Отправка уведомления о запросе на встречу к " + receiverId + " от " + senderId);
    User sender = userService.getUserByTelegramId(senderId);
    String message = stateManager.getMeetingRequestMessage(senderId);

    if (sender == null || message == null) {
      System.out.println("DEBUG: Ошибка - отправитель или сообщение не найдены");
      return;
    }

    KeyboardService keyboardService = new KeyboardService();
    ProfileService profileService = new ProfileService(userService, keyboardService);
    
    String requestInfo = profileService.formatMeetingRequest(sender, message);

    try {
      messageSender.sendTextMessageWithKeyboard(
              receiverId,
              requestInfo,
              keyboardService.createMeetingRequestKeyboard(senderId));

      if (sender.getPhotoFileId() != null && !sender.getPhotoFileId().isEmpty()) {
        messageSender.sendPhoto(receiverId, sender.getPhotoFileId(), null);
      }

      String photoFileId = stateManager.getMeetingRequestPhoto(senderId);
      if (photoFileId != null && !photoFileId.isEmpty()) {
        messageSender.sendPhoto(receiverId, photoFileId, "📸 Фото к запросу на встречу");
      }
      
      System.out.println("DEBUG: Уведомление о запросе на встречу успешно отправлено");
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке уведомления: " + e.getMessage());
      messageSender.sendTextMessage(
              receiverId,
              requestInfo + "\n\nЧтобы ответить, используйте команды:\n/accept_" + senderId + " - принять\n/decline_" + senderId + " - отклонить");
    }
  }
}