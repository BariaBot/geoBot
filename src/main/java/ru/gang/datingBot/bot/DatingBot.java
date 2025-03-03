package ru.gang.datingBot.bot;

import java.time.LocalDateTime;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.springframework.stereotype.Component;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.UserService;

import java.util.*;

@Component
public class DatingBot extends TelegramLongPollingBot {

  private final UserService userService;
  private final MeetingService meetingService;
  private final Map<Long, Integer> userLiveLocationDurations = new HashMap<>();
  private final Map<Long, Integer> userSearchRadius = new HashMap<>();
  private final Map<Long, Long> userPendingRequests = new HashMap<>();
  
  // Enum for tracking the state of conversation with users
  private enum UserState {
    NONE,
    WAITING_FOR_DESCRIPTION,
    WAITING_FOR_INTERESTS,
    WAITING_FOR_PHOTO
  }
  
  // Map to track the current state of each user
  private final Map<Long, UserState> userStates = new HashMap<>();

  public DatingBot(UserService userService, MeetingService meetingService) {
    this.userService = userService;
    this.meetingService = meetingService;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()) {
      var message = update.getMessage();
      Long chatId = message.getChatId();
      Integer messageId = message.getMessageId(); // Получаем ID сообщения

      if (message.hasText()) {
        processTextMessage(chatId, message.getText());
      } else if (message.hasLocation()) {
        processLocationMessage(chatId, message.getLocation().getLatitude(),
            message.getLocation().getLongitude(), messageId, update); // Передаем update
      } else if (message.hasPhoto()) {
        // Handle profile photo uploads
        processPhotoMessage(chatId, message.getPhoto());
      }
    }

    if (update.hasCallbackQuery()) {
      var callbackQuery = update.getCallbackQuery();
      Long chatId = callbackQuery.getMessage().getChatId();
      String data = callbackQuery.getData();
      Integer messageId = callbackQuery.getMessage().getMessageId();

      processCallbackQuery(chatId, data, messageId);
    }
  }

  private void processTextMessage(Long chatId, String text) {
    // Check user state first to handle profile creation flow
    UserState currentState = userStates.getOrDefault(chatId, UserState.NONE);
    
    switch (currentState) {
      case WAITING_FOR_DESCRIPTION:
        userService.updateUserDescription(chatId, text);
        sendTextMessage(chatId, "✅ Ваше описание обновлено! Теперь расскажите о своих интересах и хобби.");
        userStates.put(chatId, UserState.WAITING_FOR_INTERESTS);
        return;
        
      case WAITING_FOR_INTERESTS:
        userService.updateUserInterests(chatId, text);
        sendTextMessage(chatId, "✅ Ваши интересы обновлены! Теперь отправьте свое фото для профиля.");
        userStates.put(chatId, UserState.WAITING_FOR_PHOTO);
        return;
        
      case WAITING_FOR_PHOTO:
        sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию для вашего профиля.");
        return;
        
      default:
        // Process commands or regular messages
        break;
    }
    
    // Process commands
    switch (text) {
      case "/start":
        sendTimeSelection(chatId);
        break;
        
      case "/profile":
        showUserProfile(chatId);
        break;
        
      case "/edit_profile":
        startProfileEditing(chatId);
        break;
        
      case "❌ Остановить поиск":
        userService.deactivateUser(chatId);
        sendTextMessage(chatId, "Вы больше не видимы для других пользователей.");
        break;
    }
  }

  private void processLocationMessage(Long chatId, double latitude, double longitude, Integer messageId, Update update) {
    Integer duration = userLiveLocationDurations.get(chatId);
    Integer radius = userSearchRadius.get(chatId);

    if (duration != null && radius != null) {
      var from = update.getMessage().getFrom();

      String telegramUsername = (from.getUserName() != null) ? from.getUserName() : null;
      String firstName = (from.getFirstName() != null) ? from.getFirstName() : null;
      String lastName = (from.getLastName() != null) ? from.getLastName() : null;
      String phoneNumber = (update.getMessage().hasContact()) ? update.getMessage().getContact().getPhoneNumber() : null;

      userService.updateUserLocation(chatId, latitude, longitude, duration, radius, telegramUsername, firstName, lastName, phoneNumber);

      // Удаляем сообщение "Отправьте свою геолокацию..."
      deleteMessage(chatId, messageId);

      sendTextMessage(chatId, "📍 Ваше местоположение обновлено! Мы ищем для вас людей поблизости...");
      suggestNearbyUser(chatId, latitude, longitude, radius);
    } else {
      sendTextMessage(chatId, "⚠️ Пожалуйста, выберите время и радиус перед отправкой геолокации.");
    }
  }
  
  /**
   * Process profile photo uploads
   */
  private void processPhotoMessage(Long chatId, List<PhotoSize> photos) {
    if (userStates.getOrDefault(chatId, UserState.NONE) == UserState.WAITING_FOR_PHOTO) {
      // Get the largest photo (best quality)
      PhotoSize largestPhoto = photos.stream()
          .max(Comparator.comparing(PhotoSize::getFileSize))
          .orElse(null);
      
      if (largestPhoto != null) {
        // Save the file ID to the user's profile
        userService.updateUserPhoto(chatId, largestPhoto.getFileId());
        
        // Reset state and show completion message
        userStates.put(chatId, UserState.NONE);
        
        int completionPercentage = userService.getProfileCompletionPercentage(chatId);
        sendTextMessage(chatId, 
            "✅ Ваше фото профиля обновлено!\n\n" +
            "🏆 Ваш профиль заполнен на " + completionPercentage + "%\n\n" +
            "Чтобы просмотреть свой профиль, используйте команду /profile\n" +
            "Для редактирования профиля используйте /edit_profile");
      } else {
        sendTextMessage(chatId, "⚠️ Не удалось обработать фото. Пожалуйста, попробуйте еще раз.");
      }
    } else if (photos != null && !photos.isEmpty()) {
      // User sent a photo without being in profile creation flow
      sendTextMessage(chatId, "📸 Хотите обновить фото профиля? Используйте команду /edit_profile");
    }
  }

  private void processCallbackQuery(Long chatId, String data, Integer messageId) {
    // Profile related callbacks
    if (data.startsWith("edit_profile_")) {
      String field = data.replace("edit_profile_", "");
      processProfileEdit(chatId, field, messageId);
      return;
    }
    
    // Обработка выбора времени
    if (data.equals("1 час") || data.equals("3 часа") || data.equals("6 часов")) {
      int duration = Integer.parseInt(data.split(" ")[0]);
      userLiveLocationDurations.put(chatId, duration);

      // Удаляем предыдущее сообщение с кнопками
      deleteMessage(chatId, messageId);
      sendTextMessage(chatId, "✅ Вы запустили поиск людей рядом на " + duration + " часов.");

      // Отправляем выбор радиуса
      sendRadiusSelection(chatId);
    }

    // Обработка выбора радиуса
    if (data.equals("1 км") || data.equals("3 км") || data.equals("5 км") || data.equals(
        "1500 км")) {
      int radius = Integer.parseInt(data.split(" ")[0]);
      userSearchRadius.put(chatId, radius);

      // Удаляем предыдущее сообщение с кнопками
      deleteMessage(chatId, messageId);
      sendTextMessage(chatId, "📍 Вы выбрали радиус поиска " + radius + " км.");

      // Просим отправить геолокацию
      requestLiveLocation(chatId);
    }

    if (data.startsWith("send_request_")) {
      Long receiverId = Long.parseLong(data.replace("send_request_", ""));
      Long senderId = chatId;

      System.out.println("📩 Запрос на встречу от " + senderId + " к " + receiverId);

      meetingService.sendMeetingRequest(senderId, receiverId, "Привет! Давай встретимся!", LocalDateTime.now().plusHours(1));

      // Уведомляем получателя о запросе
      notifyUserAboutMeetingRequest(receiverId, senderId);

      sendTextMessage(chatId, "✅ Запрос на встречу отправлен!");
    }

    if (data.startsWith("accept_request_")) {
      Long senderId = Long.parseLong(data.replace("accept_request_", ""));
      Long receiverId = chatId;

      // Находим запрос
      List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
      for (MeetingRequest request : requests) {
        if (request.getSender().getTelegramId().equals(senderId)) {
          meetingService.acceptMeetingRequest(request.getId());
          // Уведомляем отправителя о принятии запроса
          sendTextMessage(senderId, "✅ Ваш запрос на встречу был принят!");
          sendTextMessage(chatId, "Вы приняли запрос на встречу!");
          break;
        }
      }
    }

    if (data.startsWith("decline_request_")) {
      Long senderId = Long.parseLong(data.replace("decline_request_", ""));
      Long receiverId = chatId;

      // Находим запрос
      List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
      for (MeetingRequest request : requests) {
        if (request.getSender().getTelegramId().equals(senderId)) {
          meetingService.declineMeetingRequest(request.getId());
          // Уведомляем отправителя об отклонении запроса
          sendTextMessage(senderId, "❌ Ваш запрос на встречу был отклонен.");
          sendTextMessage(chatId, "Вы отклонили запрос на встречу.");
          break;
        }
      }
    }
  }

  /**
   * Show the user's profile information
   */
  private void showUserProfile(Long chatId) {
    User user = userService.getUserByTelegramId(chatId);
    
    if (user == null) {
      sendTextMessage(chatId, "⚠️ Профиль не найден. Используйте /edit_profile, чтобы создать свой профиль.");
      return;
    }
    
    // If the user has a profile photo, send it with the profile info
    if (user.getPhotoFileId() != null && !user.getPhotoFileId().isEmpty()) {
      SendPhoto photoMessage = new SendPhoto();
      photoMessage.setChatId(chatId.toString());
      photoMessage.setPhoto(new InputFile(user.getPhotoFileId()));
      photoMessage.setCaption(user.getProfileInfo());
      photoMessage.setParseMode("Markdown");
      
      try {
        execute(photoMessage);
      } catch (TelegramApiException e) {
        e.printStackTrace();
        // Fallback to text-only if photo fails
        sendTextMessage(chatId, user.getProfileInfo());
      }
    } else {
      // Send text-only profile
      SendMessage message = new SendMessage();
      message.setChatId(chatId.toString());
      message.setText(user.getProfileInfo() + 
          "\n🔄 Используйте /edit_profile для редактирования профиля.");
      message.setParseMode("Markdown");
      executeMessage(message);
    }
    
    // Show completion percentage
    int completionPercentage = user.getProfileCompletionPercentage();
    sendTextMessage(chatId, "🏆 Ваш профиль заполнен на " + completionPercentage + "%");
    
    // Show edit button
    SendMessage editMessage = new SendMessage();
    editMessage.setChatId(chatId.toString());
    editMessage.setText("Что вы хотите изменить в своем профиле?");
    
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    
    List<InlineKeyboardButton> row1 = new ArrayList<>();
    row1.add(createButton("Описание", "edit_profile_description"));
    row1.add(createButton("Интересы", "edit_profile_interests"));
    rowsInline.add(row1);
    
    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Фото", "edit_profile_photo"));
    rowsInline.add(row2);
    
    markupInline.setKeyboard(rowsInline);
    editMessage.setReplyMarkup(markupInline);
    
    executeMessage(editMessage);
  }
  
  /**
   * Start the profile editing process
   */
  private void startProfileEditing(Long chatId) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText("Выберите, что вы хотите изменить в своем профиле:");
    
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    
    List<InlineKeyboardButton> row1 = new ArrayList<>();
    row1.add(createButton("Описание", "edit_profile_description"));
    row1.add(createButton("Интересы", "edit_profile_interests"));
    rowsInline.add(row1);
    
    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Фото", "edit_profile_photo"));
    rowsInline.add(row2);
    
    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);
    
    executeMessage(message);
  }
  
  /**
   * Process profile edit selection
   */
  private void processProfileEdit(Long chatId, String field, Integer messageId) {
    // Remove the previous message with buttons
    deleteMessage(chatId, messageId);
    
    switch (field) {
      case "description":
        sendTextMessage(chatId, "📝 Пожалуйста, напишите короткое описание о себе (до 1000 символов):");
        userStates.put(chatId, UserState.WAITING_FOR_DESCRIPTION);
        break;
        
      case "interests":
        sendTextMessage(chatId, "⭐ Пожалуйста, напишите о своих интересах и хобби (до 500 символов):");
        userStates.put(chatId, UserState.WAITING_FOR_INTERESTS);
        break;
        
      case "photo":
        sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию для вашего профиля:");
        userStates.put(chatId, UserState.WAITING_FOR_PHOTO);
        break;
    }
  }

  private void sendTimeSelection(Long chatId) {
    SendMessage message = new SendMessage(chatId.toString(),
        "Выберите, на сколько часов включить геолокацию:");
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    List<InlineKeyboardButton> rowInline = new ArrayList<>();
    rowInline.add(createButton("1 час", "1 час"));
    rowInline.add(createButton("3 часа", "3 часа"));
    rowInline.add(createButton("6 часов", "6 часов"));
    rowsInline.add(rowInline);
    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);
    executeMessage(message);
  }

  private void sendRadiusSelection(Long chatId) {
    SendMessage message = new SendMessage(chatId.toString(), "Выберите радиус поиска:");
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    List<InlineKeyboardButton> rowInline = new ArrayList<>();
    rowInline.add(createButton("1 км", "1 км"));
    rowInline.add(createButton("3 км", "3 км"));
    rowInline.add(createButton("5 км", "5 км"));
    rowInline.add(createButton("1500 км", "1500 км"));
    rowsInline.add(rowInline);
    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);
    executeMessage(message);
  }

  private void requestLiveLocation(Long chatId) {
    SendMessage message = new SendMessage(chatId.toString(),
        "Отправьте свою геолокацию, чтобы вас могли найти:");
    executeMessage(message);
  }

  private void suggestNearbyUser(Long chatId, double lat, double lon, int radius) {
    System.out.println("Поиск пользователей рядом (lat: " + lat + ", lon: " + lon + ", radius: " + radius + " км)");

    List<User> nearbyUsers = userService.findNearbyUsers(lat, lon, radius);

    if (nearbyUsers == null || nearbyUsers.isEmpty()) {
      sendTextMessage(chatId,
          "😔 На данный момент никого поблизости не найдено, попробуйте позже.\n\n" +
              "📍 У вас активна геолокация на " + userLiveLocationDurations.getOrDefault(chatId, 0) +
              " часов. Если кто-то окажется рядом, мы вам сообщим!");
      return;
    }

    System.out.println("Найдено пользователей: " + nearbyUsers.size());

    for (User user : nearbyUsers) {
      System.out.println(" - " + user.getTelegramId() + " | " +
          (user.getUsername() != null ? user.getUsername() : "без username") +
          " | " + (user.getFirstName() != null ? user.getFirstName() : "") +
          " | lat: " + user.getLatitude() + " lon: " + user.getLongitude());
    }

    // Берем первого пользователя
    User profile = nearbyUsers.get(0);

    // Если username == null, показываем заглушку
    String displayName = getDisplayName(profile);

    System.out.println("Выбран пользователь: " + profile.getTelegramId() + " | " + displayName);

    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    
    // Include profile info if available
    StringBuilder profileInfo = new StringBuilder();
    profileInfo.append("✨ @").append(displayName).append(" рядом!");
    
    if (profile.getDescription() != null && !profile.getDescription().isEmpty()) {
      profileInfo.append("\n\n📝 О себе: ").append(profile.getDescription());
    }
    
    if (profile.getInterests() != null && !profile.getInterests().isEmpty()) {
      profileInfo.append("\n\n⭐ Интересы: ").append(profile.getInterests());
    }
    
    profileInfo.append("\n\nХотите отправить запрос?");
    message.setText(profileInfo.toString());

    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    List<InlineKeyboardButton> rowInline = new ArrayList<>();

    InlineKeyboardButton sendRequestButton = new InlineKeyboardButton();
    sendRequestButton.setText("📩 Отправить запрос");
    sendRequestButton.setCallbackData("send_request_" + profile.getTelegramId());

    rowInline.add(sendRequestButton);
    rowsInline.add(rowInline);
    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);

    executeMessage(message);
    
    // If the user has a photo, send it separately
    if (profile.getPhotoFileId() != null && !profile.getPhotoFileId().isEmpty()) {
      SendPhoto photoMessage = new SendPhoto();
      photoMessage.setChatId(chatId.toString());
      photoMessage.setPhoto(new InputFile(profile.getPhotoFileId()));
      
      try {
        execute(photoMessage);
      } catch (TelegramApiException e) {
        e.printStackTrace();
      }
    }
  }

  private void notifyUserAboutMeetingRequest(Long receiverId, Long senderId) {
    User sender = userService.getUserByTelegramId(senderId);
    String senderName = getDisplayName(sender);

    SendMessage message = new SendMessage();
    message.setChatId(receiverId.toString());
    
    // Include profile info in the request notification
    StringBuilder requestInfo = new StringBuilder();
    requestInfo.append("✨ ").append(senderName).append(" отправил вам запрос на встречу!");
    
    if (sender.getDescription() != null && !sender.getDescription().isEmpty()) {
      requestInfo.append("\n\n📝 О себе: ").append(sender.getDescription());
    }
    
    if (sender.getInterests() != null && !sender.getInterests().isEmpty()) {
      requestInfo.append("\n\n⭐ Интересы: ").append(sender.getInterests());
    }
    
    message.setText(requestInfo.toString());

    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    List<InlineKeyboardButton> rowInline = new ArrayList<>();

    InlineKeyboardButton acceptButton = new InlineKeyboardButton();
    acceptButton.setText("✅ Принять");
    acceptButton.setCallbackData("accept_request_" + senderId);

    InlineKeyboardButton declineButton = new InlineKeyboardButton();
    declineButton.setText("❌ Отклонить");
    declineButton.setCallbackData("decline_request_" + senderId);

    rowInline.add(acceptButton);
    rowInline.add(declineButton);
    rowsInline.add(rowInline);
    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);

    executeMessage(message);
    
    // If the sender has a photo, send it separately
    if (sender.getPhotoFileId() != null && !sender.getPhotoFileId().isEmpty()) {
      SendPhoto photoMessage = new SendPhoto();
      photoMessage.setChatId(receiverId.toString());
      photoMessage.setPhoto(new InputFile(sender.getPhotoFileId()));
      
      try {
        execute(photoMessage);
      } catch (TelegramApiException e) {
        e.printStackTrace();
      }
    }
  }

  private void sendTextMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    executeMessage(message);
  }

  private InlineKeyboardButton createButton(String text, String callbackData) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(text);
    button.setCallbackData(callbackData);
    return button;
  }

  private void deleteMessage(Long chatId, Integer messageId) {
    try {
      execute(new DeleteMessage(chatId.toString(), messageId));
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private void executeMessage(SendMessage message) {
    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  // Метод для получения отображаемого имени пользователя
  private String getDisplayName(User user) {
    // Приоритет отображения: username -> firstName -> "Анонимный пользователь"
    if (user.getUsername() != null && !user.getUsername().isEmpty()) {
      return "@" + user.getUsername();
    } else if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
      String displayName = user.getFirstName();
      if (user.getLastName() != null && !user.getLastName().isEmpty()) {
        displayName += " " + user.getLastName();
      }
      return displayName;
    } else {
      return "Анонимный пользователь";
    }
  }

  @Override
  public String getBotUsername() {
    return "GeoGreet_bot";
  }

  @Override
  public String getBotToken() {
    return "7906499880:AAGXfaTwF3JXOsiYxIl_yvYdO696Po2DVOU";
  }
}
