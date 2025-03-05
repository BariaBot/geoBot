package ru.gang.datingBot.handler;

import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.gang.datingBot.bot.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.ProfileService;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.ChatMessage;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.ChatService;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Обработчик для сообщений чата между пользователями
 */
public class ChatHandler {

  private final UserService userService;
  private final MeetingService meetingService;
  private final ChatService chatService;
  private final UserStateManager stateManager;
  private final MessageSender messageSender;
  private final KeyboardService keyboardService;

  public ChatHandler(
          UserService userService,
          MeetingService meetingService,
          ChatService chatService,
          UserStateManager stateManager,
          MessageSender messageSender) {
    this.userService = userService;
    this.meetingService = meetingService;
    this.chatService = chatService;
    this.stateManager = stateManager;
    this.messageSender = messageSender;
    this.keyboardService = new KeyboardService();
  }

  /**
   * Обрабатывает текстовые сообщения в режиме чата
   */
  public void processChatMessage(Long chatId, String text) {
    // Получаем информацию о текущем чате пользователя
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
      // Отправляем сообщение через сервис
      chatService.sendMessage(chatId, targetUserId, meetingRequestId, text, null);
      
      // Доставляем сообщение целевому пользователю
      User sender = userService.getUserByTelegramId(chatId);
      String senderName = getSenderDisplayName(sender);
      
      // Уведомляем получателя о новом сообщении
      messageSender.sendTextMessage(
              targetUserId,
              "💬 " + senderName + ": " + text);
      
      System.out.println("DEBUG: Сообщение отправлено от " + chatId + " к " + targetUserId + ": " + text);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке сообщения: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отправке сообщения.");
    }
  }
  
  /**
   * Обрабатывает фотографии в режиме чата
   */
  public void processChatPhoto(Long chatId, List<PhotoSize> photos) {
    // Получаем информацию о текущем чате пользователя
    Long targetUserId = stateManager.getCurrentChatUser(chatId);
    Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);
    
    if (targetUserId == null || meetingRequestId == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Чат не активен. Выберите пользователя для общения из списка принятых встреч.");
      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }
    
    // Получаем самое большое фото (лучшее качество)
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
      // Отправляем сообщение через сервис
      chatService.sendMessage(chatId, targetUserId, meetingRequestId, "📸 Фото", fileId);
      
      // Доставляем фото целевому пользователю
      User sender = userService.getUserByTelegramId(chatId);
      String senderName = getSenderDisplayName(sender);
      
      // Уведомляем получателя о новом фото
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

  /**
   * Обрабатывает стикеры в режиме чата
   */
  public void processChatSticker(Long chatId, Object sticker) {
    // Получаем информацию о текущем чате пользователя
    Long targetUserId = stateManager.getCurrentChatUser(chatId);
    Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);
    
    if (targetUserId == null || meetingRequestId == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Чат не активен. Выберите пользователя для общения из списка принятых встреч.");
      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }

    // Используем рефлексию для получения fileId из объекта стикера
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
      // Отправляем сообщение через сервис с типом "sticker"
      chatService.sendMessage(chatId, targetUserId, meetingRequestId, "sticker", fileId);
      
      // Доставляем стикер целевому пользователю
      User sender = userService.getUserByTelegramId(chatId);
      
      // Отправляем стикер
      messageSender.sendSticker(targetUserId, fileId);
      
      System.out.println("DEBUG: Стикер отправлен от " + chatId + " к " + targetUserId);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке стикера: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отправке стикера.");
    }
  }
  
  /**
   * Единый метод для обработки других типов медиафайлов
   */
  public void processMediaMessage(Long chatId, String mediaType, String fileId, String caption) {
    // Получаем информацию о текущем чате
    Long targetUserId = stateManager.getCurrentChatUser(chatId);
    Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);
    
    if (targetUserId == null || meetingRequestId == null) {
      messageSender.sendTextMessage(chatId, "⚠️ Чат не активен.");
      stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
      return;
    }
    
    try {
      // Отправляем сообщение через сервис
      chatService.sendMessage(chatId, targetUserId, meetingRequestId, mediaType, fileId);
      
      // Доставляем медиа получателю
      User sender = userService.getUserByTelegramId(chatId);
      String senderName = getSenderDisplayName(sender);
      
      // Формируем подпись
      String mediaCaption = caption;
      if (caption == null || caption.isEmpty()) {
        mediaCaption = mediaType + " от " + senderName;
      } else {
        mediaCaption = caption + " от " + senderName;
      }
      
      // Отправляем соответствующий тип медиа
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
          // Если тип неизвестен, просто отправляем ссылку на медиафайл
          messageSender.sendTextMessage(targetUserId, "📎 " + senderName + " отправил вам медиафайл типа " + mediaType);
      }
      
      System.out.println("DEBUG: Медиафайл типа " + mediaType + " отправлен от " + chatId + " к " + targetUserId);
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при отправке медиафайла: " + e.getMessage());
      messageSender.sendTextMessage(chatId, "❌ Произошла ошибка при отправке медиафайла.");
    }
  }
  
  /**
   * Обработчик для обработки анимаций/GIF
   */
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
  
  /**
   * Обработчик для обработки видео
   */
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
  
  /**
   * Обработчик для обработки голосовых сообщений
   */
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
  
  /**
   * Обработчик для обработки аудио сообщений
   */
  public void processChatAudio(Long chatId, Object audio) {
    try {
        java.lang.reflect.Method getFileId = audio.getClass().getMethod("getFileId");
        String fileId = (String) getFileId.invoke(audio);
        
        // Попробуем получить название трека, если есть
        String title = null;
        try {
            java.lang.reflect.Method getTitle = audio.getClass().getMethod("getTitle");
            title = (String) getTitle.invoke(audio);
        } catch (Exception e) {
            // Если не получилось получить название, игнорируем
        }
        
        String caption = (title != null && !title.isEmpty()) ? "Аудио: " + title : "Аудио";
        processMediaMessage(chatId, "audio", fileId, caption);
    } catch (Exception e) {
        System.out.println("DEBUG: Ошибка при обработке аудио: " + e.getMessage());
        messageSender.sendTextMessage(chatId, "❌ Не удалось обработать аудио.");
    }
  }
  
  /**
   * Обработчик для обработки документов
   */
  public void processChatDocument(Long chatId, Object document) {
    try {
        java.lang.reflect.Method getFileId = document.getClass().getMethod("getFileId");
        String fileId = (String) getFileId.invoke(document);
        
        // Попробуем получить имя файла, если есть
        String fileName = null;
        try {
            java.lang.reflect.Method getFileName = document.getClass().getMethod("getFileName");
            fileName = (String) getFileName.invoke(document);
        } catch (Exception e) {
            // Если не получилось получить имя файла, игнорируем
        }
        
        String caption = (fileName != null && !fileName.isEmpty()) ? "Документ: " + fileName : "Документ";
        processMediaMessage(chatId, "document", fileId, caption);
    } catch (Exception e) {
        System.out.println("DEBUG: Ошибка при обработке документа: " + e.getMessage());
        messageSender.sendTextMessage(chatId, "❌ Не удалось обработать документ.");
    }
  }
  
  /**
   * Инициирует чат между пользователями после принятия запроса на встречу
   */
  public void initializeChat(Long senderUserId, Long receiverUserId, Long meetingRequestId) {
    // Получаем имена пользователей для персонализации сообщений
    User sender = userService.getUserByTelegramId(senderUserId);
    User receiver = userService.getUserByTelegramId(receiverUserId);
    
    String senderName = getSenderDisplayName(sender);
    String receiverName = getSenderDisplayName(receiver);
    
    // Уведомляем отправителя запроса
    String senderMessage = "✅ " + receiverName + " принял(а) ваш запрос на встречу!\n\n" +
                           "Теперь вы можете обмениваться сообщениями. Все ваши сообщения будут доставлены собеседнику.\n\n" +
                           "Для завершения чата введите /end_chat";
    
    messageSender.sendTextMessage(senderUserId, senderMessage);
    
    // Уведомляем принявшего запрос
    String receiverMessage = "✅ Вы приняли запрос на встречу от " + senderName + "!\n\n" +
                            "Теперь вы можете обмениваться сообщениями. Все ваши сообщения будут доставлены собеседнику.\n\n" +
                            "Для завершения чата введите /end_chat";
    
    messageSender.sendTextMessage(receiverUserId, receiverMessage);
    
    // Устанавливаем состояние чата для обоих пользователей
    stateManager.startChatting(senderUserId, receiverUserId, meetingRequestId);
    stateManager.startChatting(receiverUserId, senderUserId, meetingRequestId);
    
    System.out.println("DEBUG: Чат инициализирован между " + senderUserId + " и " + receiverUserId + " для запроса " + meetingRequestId);
  }
  
  /**
   * Завершает текущий чат
   */
  public void endCurrentChat(Long chatId) {
    Long targetUserId = stateManager.getCurrentChatUser(chatId);
    
    if (targetUserId == null) {
      messageSender.sendTextMessage(chatId, "⚠️ У вас нет активного чата.");
      return;
    }
    
    // Сбрасываем состояние чата
    stateManager.endChatting(chatId);
    
    messageSender.sendTextMessageWithKeyboard(
            chatId,
            "Чат завершен. Вы вернулись в главное меню.",
            keyboardService.createMainKeyboard());
    
    // Уведомляем собеседника
    try {
      User user = userService.getUserByTelegramId(chatId);
      String userName = getSenderDisplayName(user);
      
      messageSender.sendTextMessage(
              targetUserId,
              userName + " завершил(а) чат. Вы можете начать новый чат в любое время.");
    } catch (Exception e) {
      System.out.println("DEBUG: Ошибка при уведомлении о завершении чата: " + e.getMessage());
    }
  }
  
  /**
   * Возвращает отображаемое имя пользователя
   */
  private String getSenderDisplayName(User user) {
    if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
      return user.getFirstName();
    } else if (user.getUsername() != null && !user.getUsername().isEmpty()) {
      return "@" + user.getUsername();
    } else {
      return "Пользователь";
    }
  }
}