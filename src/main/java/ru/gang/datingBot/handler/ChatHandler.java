package ru.gang.datingBot.handler;

import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.gang.datingBot.service.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.ChatService;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.ProfileService;
import ru.gang.datingBot.service.UserService;

import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChatHandler {

  private final UserService userService;
  private final MeetingService meetingService;
  private final ChatService chatService;
  private final UserStateManager stateManager;
  private final MessageSender messageSender;
  private final KeyboardService keyboardService;
  private final ProfileService profileService;

  public void processChatMessage(Long chatId, String text) {
    Long targetUserId = stateManager.getCurrentChatUser(chatId);
    Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);

    if (targetUserId == null || meetingRequestId == null) {
      messageSender.sendTextMessageWithKeyboard(
              chatId,
              "⚠️ Чат не активен. Выберите пользователя для общения из списка принятых встреч.",
              keyboardService.createMainKeyboard());
      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }

    try {
      chatService.sendMessage(chatId, targetUserId, meetingRequestId, text, null);

      User sender = userService.getUserByTelegramId(chatId);
      String senderName = profileService.getSenderDisplayName(sender);

      messageSender.sendTextMessage(
              targetUserId,
              "💬 " + senderName + ": " + text);

      System.out.println("DEBUG: Сообщение отправлено от " + chatId + " к " + targetUserId + ": " + text);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке сообщения: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отправке сообщения.");
    }
  }

  public void processChatPhoto(Long chatId, List<PhotoSize> photos) {
    Long targetUserId = stateManager.getCurrentChatUser(chatId);
    Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);

    if (targetUserId == null || meetingRequestId == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Чат не активен. Выберите пользователя для общения из списка принятых встреч.");
      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }

    PhotoSize largestPhoto = photos.stream()
            .max(Comparator.comparing(PhotoSize::getFileSize))
            .orElse(null);

    if (largestPhoto == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Не удалось обработать фото. Пожалуйста, попробуйте еще раз.");
      return;
    }

    String fileId = largestPhoto.getFileId();
    System.out.println("DEBUG: Получено фото в чате с fileId: " + fileId);

    try {
      chatService.sendMessage(chatId, targetUserId, meetingRequestId, "📸 Фото", fileId);

      User sender = userService.getUserByTelegramId(chatId);
      String senderName = profileService.getSenderDisplayName(sender);

      messageSender.sendPhoto(
              targetUserId,
              fileId,
              "📸 Фото от " + senderName);

      System.out.println("DEBUG: Фото отправлено от " + chatId + " к " + targetUserId);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке фото: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отправке фото.");
    }
  }

  public void processChatSticker(Long chatId, Object sticker) {
    Long targetUserId = stateManager.getCurrentChatUser(chatId);
    Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);

    if (targetUserId == null || meetingRequestId == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Чат не активен. Выберите пользователя для общения из списка принятых встреч.");
      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }

    String fileId = null;
    try {
      java.lang.reflect.Method getFileId = sticker.getClass().getMethod("getFileId");
      fileId = (String) getFileId.invoke(sticker);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при получении fileId стикера: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Не удалось обработать стикер.");
      return;
    }

    System.out.println("DEBUG: Получен стикер в чате с fileId: " + fileId);

    try {
      chatService.sendMessage(chatId, targetUserId, meetingRequestId, "sticker", fileId);

      User sender = userService.getUserByTelegramId(chatId);

      messageSender.sendSticker(targetUserId, fileId);

      System.out.println("DEBUG: Стикер отправлен от " + chatId + " к " + targetUserId);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке стикера: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отправке стикера.");
    }
  }

  public void processMediaMessage(Long chatId, String mediaType, String fileId, String caption) {
    Long targetUserId = stateManager.getCurrentChatUser(chatId);
    Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);

    if (targetUserId == null || meetingRequestId == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Чат не активен.");
      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }

    try {
      chatService.sendMessage(chatId, targetUserId, meetingRequestId, mediaType, fileId);

      User sender = userService.getUserByTelegramId(chatId);
      String senderName = profileService.getSenderDisplayName(sender);

      String mediaCaption = caption;
      if (caption == null || caption.isEmpty()) {
        mediaCaption = mediaType + " от " + senderName;
      } else {
        mediaCaption = caption + " от " + senderName;
      }

      switch (mediaType) {
        case "video":
          messageSender.sendVideo(targetUserId, fileId, mediaCaption);
          break;
        case "voice":
          messageSender.sendVoice(targetUserId, fileId, mediaCaption);
          break;
        case "audio":
          messageSender.sendAudio(targetUserId, fileId, mediaCaption);
          break;
        case "document":
          messageSender.sendDocument(targetUserId, fileId, mediaCaption);
          break;
        case "animation":
          messageSender.sendAnimation(targetUserId, fileId, mediaCaption);
          break;
        default:
          messageSender.sendTextMessage(targetUserId, "📎 " + senderName + " отправил вам медиафайл типа " + mediaType);
      }

      System.out.println("DEBUG: Медиафайл типа " + mediaType + " отправлен от " + chatId + " к " + targetUserId);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке медиафайла: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отправке медиафайла.");
    }
  }

  public void processChatAnimation(Long chatId, Object animation) {
    try {
      java.lang.reflect.Method getFileId = animation.getClass().getMethod("getFileId");
      String fileId = (String) getFileId.invoke(animation);
      processMediaMessage(chatId, "animation", fileId, "GIF");
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при обработке анимации: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Не удалось обработать анимацию.");
    }
  }

  public void processChatVideo(Long chatId, Object video) {
    try {
      java.lang.reflect.Method getFileId = video.getClass().getMethod("getFileId");
      String fileId = (String) getFileId.invoke(video);
      processMediaMessage(chatId, "video", fileId, "Видео");
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при обработке видео: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Не удалось обработать видео.");
    }
  }

  public void processChatVoice(Long chatId, Object voice) {
    try {
      java.lang.reflect.Method getFileId = voice.getClass().getMethod("getFileId");
      String fileId = (String) getFileId.invoke(voice);
      processMediaMessage(chatId, "voice", fileId, "Голосовое сообщение");
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при обработке голосового сообщения: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Не удалось обработать голосовое сообщение.");
    }
  }

  public void processChatAudio(Long chatId, Object audio) {
    try {
      java.lang.reflect.Method getFileId = audio.getClass().getMethod("getFileId");
      String fileId = (String) getFileId.invoke(audio);

      String title = null;
      try {
        java.lang.reflect.Method getTitle = audio.getClass().getMethod("getTitle");
        title = (String) getTitle.invoke(audio);
      } catch (Exception e) {
      }

      String caption = (title != null && !title.isEmpty()) ? "Аудио: " + title : "Аудио";
      processMediaMessage(chatId, "audio", fileId, caption);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при обработке аудио: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Не удалось обработать аудио.");
    }
  }

  public void processChatDocument(Long chatId, Object document) {
    try {
      java.lang.reflect.Method getFileId = document.getClass().getMethod("getFileId");
      String fileId = (String) getFileId.invoke(document);

      String fileName = null;
      try {
        java.lang.reflect.Method getFileName = document.getClass().getMethod("getFileName");
        fileName = (String) getFileName.invoke(document);
      } catch (Exception e) {
      }

      String caption = (fileName != null && !fileName.isEmpty()) ? "Документ: " + fileName : "Документ";
      processMediaMessage(chatId, "document", fileId, caption);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при обработке документа: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Не удалось обработать документ.");
    }
  }

  public void initializeChat(Long senderUserId, Long receiverUserId, Long meetingRequestId) {
    User sender = userService.getUserByTelegramId(senderUserId);
    User receiver = userService.getUserByTelegramId(receiverUserId);

    String senderName = profileService.getSenderDisplayName(sender);
    String receiverName = profileService.getSenderDisplayName(receiver);

    String senderMessage = "✅ " + receiverName + " принял(а) ваш запрос на встречу!\n\n" +
            "Теперь вы можете обмениваться сообщениями. Все ваши сообщения будут доставлены собеседнику.\n\n" +
            "Для завершения чата введите /end_chat";

    messageSender.sendTextMessage(senderUserId, senderMessage);

    String receiverMessage = "✅ Вы приняли запрос на встречу от " + senderName + "!\n\n" +
            "Теперь вы можете обмениваться сообщениями. Все ваши сообщения будут доставлены собеседнику.\n\n" +
            "Для завершения чата введите /end_chat";

    messageSender.sendTextMessage(receiverUserId, receiverMessage);

    stateManager.startChatting(senderUserId, receiverUserId, meetingRequestId);
    stateManager.startChatting(receiverUserId, senderUserId, meetingRequestId);

    System.out.println("DEBUG: Чат инициализирован между " + senderUserId + " и " + receiverUserId + " для запроса " + meetingRequestId);
  }

  public void endCurrentChat(Long chatId) {
    Long targetUserId = stateManager.getCurrentChatUser(chatId);

    if (targetUserId == null) {
      messageSender.sendTextMessage(chatId, "⚠️ У вас нет активного чата.");
      return;
    }

    stateManager.endChatting(chatId);

    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Чат завершен. Вы вернулись в главное меню.",
            keyboardService.createMainKeyboard());

    try {
      User user = userService.getUserByTelegramId(chatId);
      String userName = profileService.getSenderDisplayName(user);

      messageSender.sendTextMessage(
              targetUserId,
              userName + " завершил(а) чат. Вы можете начать новый чат в любое время.");
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при уведомлении о завершении чата: " + e.getMessage());
    }
  }
}