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
  
  // Хранилище для кэширования результатов поиска
  private final Map<Long, List<User>> nearbyUsersCache = new HashMap<>();
  private final Map<Long, Integer> currentUserIndexCache = new HashMap<>();
  
  // Хранилище временных данных для создания запроса на встречу
  private final Map<Long, String> meetingRequestMessages = new HashMap<>();
  private final Map<Long, String> meetingRequestPhotos = new HashMap<>();
  private final Map<Long, Long> meetingRequestTargets = new HashMap<>();
  
  // Enum для отслеживания состояния разговора с пользователями
  private enum UserState {
    NONE,
    WAITING_FOR_DESCRIPTION,
    WAITING_FOR_INTERESTS,
    WAITING_FOR_PHOTO,
    WAITING_FOR_AGE,
    WAITING_FOR_GENDER,
    WAITING_FOR_MIN_AGE,
    WAITING_FOR_MAX_AGE,
    WAITING_FOR_GENDER_PREFERENCE,
    WAITING_FOR_MEETING_MESSAGE,
    WAITING_FOR_MEETING_PHOTO
  }
  
  // Карта для отслеживания текущего состояния каждого пользователя
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
        // Обработка загрузки фото профиля или фото для запроса на встречу
        processPhotoMessage(chatId, message.getPhoto(), messageId);
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
    // Проверяем состояние пользователя в первую очередь для обработки процесса создания профиля
    UserState currentState = userStates.getOrDefault(chatId, UserState.NONE);
    
    switch (currentState) {
      case WAITING_FOR_DESCRIPTION:
        userService.updateUserDescription(chatId, text);
        sendTextMessage(chatId, "✅ Ваше описание обновлено! Теперь расскажите о своих интересах и хобби.");
        userStates.put(chatId, UserState.WAITING_FOR_INTERESTS);
        return;
        
      case WAITING_FOR_INTERESTS:
        userService.updateUserInterests(chatId, text);
        sendTextMessage(chatId, "✅ Ваши интересы обновлены! Теперь укажите ваш возраст.");
        userStates.put(chatId, UserState.WAITING_FOR_AGE);
        return;
        
      case WAITING_FOR_AGE:
        try {
          int age = Integer.parseInt(text.trim());
          if (age < 18 || age > 100) {
            sendTextMessage(chatId, "⚠️ Пожалуйста, введите корректный возраст (от 18 до 100 лет).");
            return;
          }
          userService.updateUserAge(chatId, age);
          sendGenderSelection(chatId);
          userStates.put(chatId, UserState.WAITING_FOR_GENDER);
        } catch (NumberFormatException e) {
          sendTextMessage(chatId, "⚠️ Пожалуйста, введите возраст числом.");
        }
        return;
        
      case WAITING_FOR_MIN_AGE:
        try {
          int minAge = Integer.parseInt(text.trim());
          if (minAge < 18 || minAge > 100) {
            sendTextMessage(chatId, "⚠️ Пожалуйста, введите корректный минимальный возраст (от 18 до 100 лет).");
            return;
          }
          
          User user = userService.getUserByTelegramId(chatId);
          // Временно сохраняем только минимальный возраст
          userService.updateUserSearchPreferences(chatId, minAge, user.getMaxAgePreference(), user.getGenderPreference());
          
          sendTextMessage(chatId, "✅ Минимальный возраст установлен на " + minAge + " лет. Теперь укажите максимальный возраст для поиска.");
          userStates.put(chatId, UserState.WAITING_FOR_MAX_AGE);
        } catch (NumberFormatException e) {
          sendTextMessage(chatId, "⚠️ Пожалуйста, введите возраст числом.");
        }
        return;
        
      case WAITING_FOR_MAX_AGE:
        try {
          int maxAge = Integer.parseInt(text.trim());
          if (maxAge < 18 || maxAge > 100) {
            sendTextMessage(chatId, "⚠️ Пожалуйста, введите корректный максимальный возраст (от 18 до 100 лет).");
            return;
          }
          
          User user = userService.getUserByTelegramId(chatId);
          if (user.getMinAgePreference() != null && maxAge < user.getMinAgePreference()) {
            sendTextMessage(chatId, "⚠️ Максимальный возраст должен быть больше или равен минимальному возрасту " + user.getMinAgePreference() + " лет.");
            return;
          }
          
          // Теперь сохраняем оба значения возраста
          userService.updateUserSearchPreferences(chatId, user.getMinAgePreference(), maxAge, user.getGenderPreference());
          
          sendGenderPreferenceSelection(chatId);
          userStates.put(chatId, UserState.WAITING_FOR_GENDER_PREFERENCE);
        } catch (NumberFormatException e) {
          sendTextMessage(chatId, "⚠️ Пожалуйста, введите возраст числом.");
        }
        return;
        
      case WAITING_FOR_PHOTO:
        sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию для вашего профиля.");
        return;
        
      case WAITING_FOR_MEETING_MESSAGE:
        // Сохраняем сообщение для запроса на встречу
        meetingRequestMessages.put(chatId, text);
        sendTextMessage(chatId, "✅ Сообщение сохранено! Хотите добавить фото к запросу? (отправьте фото или напишите \"нет\")");
        userStates.put(chatId, UserState.WAITING_FOR_MEETING_PHOTO);
        return;
        
      case WAITING_FOR_MEETING_PHOTO:
        if (text.equalsIgnoreCase("нет") || text.equalsIgnoreCase("no")) {
          // Отправляем запрос без фото
          Long targetUserId = meetingRequestTargets.get(chatId);
          String message = meetingRequestMessages.get(chatId);
          
          if (targetUserId != null && message != null) {
            meetingService.sendMeetingRequest(chatId, targetUserId, message, LocalDateTime.now().plusHours(1));
            
            // Уведомляем получателя о запросе
            notifyUserAboutMeetingRequest(targetUserId, chatId);
            
            sendTextMessage(chatId, "✅ Запрос на встречу отправлен!");
            
            // Очищаем временные данные
            meetingRequestMessages.remove(chatId);
            meetingRequestTargets.remove(chatId);
          } else {
            sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
          }
          
          userStates.put(chatId, UserState.NONE);
        } else {
          sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию или напишите \"нет\", если не хотите добавлять фото.");
        }
        return;
        
      default:
        // Обработка команд или обычных сообщений
        break;
    }
    
    // Обработка команд
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
        
      case "/search_settings":
        showSearchSettings(chatId);
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
      
      // Поиск пользователей поблизости с учетом фильтров
      List<User> nearbyUsers = userService.findNearbyUsers(chatId, latitude, longitude, radius);
      
      // Кэшируем результаты поиска
      nearbyUsersCache.put(chatId, nearbyUsers);
      currentUserIndexCache.put(chatId, 0); // Начинаем с первого пользователя
      
      // Показываем первого пользователя из списка
      showCurrentNearbyUser(chatId);
    } else {
      sendTextMessage(chatId, "⚠️ Пожалуйста, выберите время и радиус перед отправкой геолокации.");
    }
  }
  
  /**
   * Обработка загрузки фотографий
   */
  private void processPhotoMessage(Long chatId, List<PhotoSize> photos, Integer messageId) {
    UserState currentState = userStates.getOrDefault(chatId, UserState.NONE);
    
    // Получаем самое большое фото (лучшее качество)
    PhotoSize largestPhoto = photos.stream()
        .max(Comparator.comparing(PhotoSize::getFileSize))
        .orElse(null);
    
    if (largestPhoto == null) {
      sendTextMessage(chatId, "⚠️ Не удалось обработать фото. Пожалуйста, попробуйте еще раз.");
      return;
    }
    
    String fileId = largestPhoto.getFileId();
    
    switch (currentState) {
      case WAITING_FOR_PHOTO:
        // Сохраняем фото профиля
        userService.updateUserPhoto(chatId, fileId);
        
        int completionPercentage = userService.getProfileCompletionPercentage(chatId);
        sendTextMessage(chatId, 
            "✅ Ваше фото профиля обновлено!\n\n" +
            "🏆 Ваш профиль заполнен на " + completionPercentage + "%\n\n" +
            "Чтобы просмотреть свой профиль, используйте команду /profile\n" +
            "Для редактирования профиля используйте /edit_profile");
        
        userStates.put(chatId, UserState.NONE);
        break;
        
      case WAITING_FOR_MEETING_PHOTO:
        // Сохраняем фото для запроса на встречу
        meetingRequestPhotos.put(chatId, fileId);
        
        Long targetUserId = meetingRequestTargets.get(chatId);
        String message = meetingRequestMessages.get(chatId);
        
        if (targetUserId != null && message != null) {
          meetingService.sendMeetingRequest(chatId, targetUserId, message, LocalDateTime.now().plusHours(1), fileId);
          
          // Уведомляем получателя о запросе
          notifyUserAboutMeetingRequest(targetUserId, chatId);
          
          sendTextMessage(chatId, "✅ Запрос на встречу с фото отправлен!");
          
          // Очищаем временные данные
          meetingRequestMessages.remove(chatId);
          meetingRequestPhotos.remove(chatId);
          meetingRequestTargets.remove(chatId);
        } else {
          sendTextMessage(chatId, "❌ Произошла ошибка. Пожалуйста, попробуйте снова.");
        }
        
        userStates.put(chatId, UserState.NONE);
        break;
        
      default:
        // Пользователь отправил фото вне контекста создания профиля
        sendTextMessage(chatId, "📸 Хотите обновить фото профиля? Используйте команду /edit_profile");
        break;
    }
  }

  private void processCallbackQuery(Long chatId, String data, Integer messageId) {
    // Обработка команд, связанных с профилем
    if (data.startsWith("edit_profile_")) {
      String field = data.replace("edit_profile_", "");
      processProfileEdit(chatId, field, messageId);
      return;
    }
    
    // Обработка выбора пола
    if (data.startsWith("gender_")) {
      String gender = data.replace("gender_", "");
      userService.updateUserGender(chatId, gender);
      
      deleteMessage(chatId, messageId);
      sendTextMessage(chatId, "✅ Ваш пол установлен: " + getGenderDisplay(gender));
      
      sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию для вашего профиля:");
      userStates.put(chatId, UserState.WAITING_FOR_PHOTO);
      return;
    }
    
    // Обработка выбора предпочтений по полу
    if (data.startsWith("gender_pref_")) {
      String genderPref = data.replace("gender_pref_", "");
      
      User user = userService.getUserByTelegramId(chatId);
      userService.updateUserSearchPreferences(chatId, user.getMinAgePreference(), user.getMaxAgePreference(), genderPref);
      
      deleteMessage(chatId, messageId);
      sendTextMessage(chatId, "✅ Настройки поиска обновлены!\n\n" +
                             "🔍 Возраст: " + user.getMinAgePreference() + " - " + user.getMaxAgePreference() + " лет\n" +
                             "👥 Пол: " + getGenderPreferenceDisplay(genderPref));
      
      userStates.put(chatId, UserState.NONE);
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
    if (data.equals("1 км") || data.equals("3 км") || data.equals("5 км") || data.equals("1500 км")) {
      int radius = Integer.parseInt(data.split(" ")[0]);
      userSearchRadius.put(chatId, radius);

      // Удаляем предыдущее сообщение с кнопками
      deleteMessage(chatId, messageId);
      sendTextMessage(chatId, "📍 Вы выбрали радиус поиска " + radius + " км.");

      // Просим отправить геолокацию
      requestLiveLocation(chatId);
    }

    // Отправка запроса на встречу
    if (data.startsWith("send_request_")) {
      Long receiverId = Long.parseLong(data.replace("send_request_", ""));
      meetingRequestTargets.put(chatId, receiverId);
      
      deleteMessage(chatId, messageId);
      sendTextMessage(chatId, "📝 Напишите сообщение для запроса на встречу:");
      userStates.put(chatId, UserState.WAITING_FOR_MEETING_MESSAGE);
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
          sendTextMessage(senderId, "✅ Ваш запрос на встречу был принят!");
          sendTextMessage(chatId, "Вы приняли запрос на встречу!");
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
          sendTextMessage(senderId, "❌ Ваш запрос на встречу был отклонен.");
          sendTextMessage(chatId, "Вы отклонили запрос на встречу.");
          break;
        }
      }
    }
  }

  /**
   * Показывает профиль пользователя
   */
  private void showUserProfile(Long chatId) {
    User user = userService.getUserByTelegramId(chatId);
    
    if (user == null) {
      sendTextMessage(chatId, "⚠️ Профиль не найден. Используйте /edit_profile, чтобы создать свой профиль.");
      return;
    }
    
    // Если у пользователя есть фото профиля, отправляем его с информацией профиля
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
        // Запасной вариант - только текст, если фото не загружается
        sendTextMessage(chatId, user.getProfileInfo());
      }
    } else {
      // Отправляем профиль только с текстом
      SendMessage message = new SendMessage();
      message.setChatId(chatId.toString());
      message.setText(user.getProfileInfo() + 
          "\n🔄 Используйте /edit_profile для редактирования профиля.");
      message.setParseMode("Markdown");
      executeMessage(message);
    }
    
    // Показываем процент заполненности
    int completionPercentage = user.getProfileCompletionPercentage();
    sendTextMessage(chatId, "🏆 Ваш профиль заполнен на " + completionPercentage + "%");
    
    // Показываем кнопку редактирования
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
    row2.add(createButton("Возраст", "edit_profile_age"));
    row2.add(createButton("Пол", "edit_profile_gender"));
    rowsInline.add(row2);
    
    List<InlineKeyboardButton> row3 = new ArrayList<>();
    row3.add(createButton("Фото", "edit_profile_photo"));
    row3.add(createButton("Настройки поиска", "edit_profile_search"));
    rowsInline.add(row3);
    
    markupInline.setKeyboard(rowsInline);
    editMessage.setReplyMarkup(markupInline);
    
    executeMessage(editMessage);
  }
  
  /**
   * Начинаем процесс редактирования профиля
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
    row2.add(createButton("Возраст", "edit_profile_age"));
    row2.add(createButton("Пол", "edit_profile_gender"));
    rowsInline.add(row2);
    
    List<InlineKeyboardButton> row3 = new ArrayList<>();
    row3.add(createButton("Фото", "edit_profile_photo"));
    row3.add(createButton("Настройки поиска", "edit_profile_search"));
    rowsInline.add(row3);
    
    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);
    
    executeMessage(message);
  }
  
  /**
   * Отображает настройки поиска
   */
  private void showSearchSettings(Long chatId) {
    User user = userService.getUserByTelegramId(chatId);
    
    if (user == null) {
      sendTextMessage(chatId, "⚠️ Профиль не найден. Используйте /edit_profile, чтобы создать свой профиль.");
      return;
    }
    
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    
    StringBuilder settingsInfo = new StringBuilder();
    settingsInfo.append("🔍 *Ваши настройки поиска:*\n\n");
    
    // Информация о возрастном диапазоне
    String ageRange = "Любой";
    if (user.getMinAgePreference() != null && user.getMaxAgePreference() != null) {
      ageRange = user.getMinAgePreference() + " - " + user.getMaxAgePreference() + " лет";
    } else if (user.getMinAgePreference() != null) {
      ageRange = "от " + user.getMinAgePreference() + " лет";
    } else if (user.getMaxAgePreference() != null) {
      ageRange = "до " + user.getMaxAgePreference() + " лет";
    }
    
    settingsInfo.append("🎯 *Возраст:* ").append(ageRange).append("\n");
    settingsInfo.append("👥 *Пол:* ").append(user.getGenderPreferenceDisplay()).append("\n");
    settingsInfo.append("📍 *Радиус поиска:* ").append(user.getSearchRadius()).append(" км\n");
    
    message.setText(settingsInfo.toString());
    message.setParseMode("Markdown");
    executeMessage(message);
    
    // Показываем кнопки для изменения настроек
    SendMessage editMessage = new SendMessage();
    editMessage.setChatId(chatId.toString());
    editMessage.setText("Что вы хотите изменить?");
    
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    
    List<InlineKeyboardButton> row1 = new ArrayList<>();
    row1.add(createButton("Возрастной диапазон", "edit_profile_age_range"));
    rowsInline.add(row1);
    
    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Предпочитаемый пол", "edit_profile_gender_pref"));
    rowsInline.add(row2);
    
    markupInline.setKeyboard(rowsInline);
    editMessage.setReplyMarkup(markupInline);
    
    executeMessage(editMessage);
  }

  /**
   * Обработка выбора редактирования профиля
   */
  private void processProfileEdit(Long chatId, String field, Integer messageId) {
    // Удаляем предыдущее сообщение с кнопками
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

      case "age":
        sendTextMessage(chatId, "🎂 Пожалуйста, введите ваш возраст (число от 18 до 100):");
        userStates.put(chatId, UserState.WAITING_FOR_AGE);
        break;

      case "gender":
        sendGenderSelection(chatId);
        userStates.put(chatId, UserState.WAITING_FOR_GENDER);
        break;

      case "photo":
        sendTextMessage(chatId, "📸 Пожалуйста, отправьте фотографию для вашего профиля:");
        userStates.put(chatId, UserState.WAITING_FOR_PHOTO);
        break;

      case "search":
        showSearchSettings(chatId);
        break;

      case "age_range":
        sendTextMessage(chatId, "🎯 Пожалуйста, введите минимальный возраст для поиска (число от 18 до 100):");
        userStates.put(chatId, UserState.WAITING_FOR_MIN_AGE);
        break;

      case "gender_pref":
        sendGenderPreferenceSelection(chatId);
        userStates.put(chatId, UserState.WAITING_FOR_GENDER_PREFERENCE);
        break;
    }
  }

  /**
   * Отправляет меню выбора времени
   */
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

  /**
   * Отправляет меню выбора радиуса
   */
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

  /**
   * Отправляет меню выбора пола
   */
  private void sendGenderSelection(Long chatId) {
    SendMessage message = new SendMessage(chatId.toString(), "Выберите ваш пол:");
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    List<InlineKeyboardButton> rowInline = new ArrayList<>();
    rowInline.add(createButton("Мужской", "gender_male"));
    rowInline.add(createButton("Женский", "gender_female"));
    rowsInline.add(rowInline);

    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Другой", "gender_other"));
    rowsInline.add(row2);

    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);
    executeMessage(message);
  }

  /**
   * Отправляет меню выбора предпочитаемого пола
   */
  private void sendGenderPreferenceSelection(Long chatId) {
    SendMessage message = new SendMessage(chatId.toString(), "Выберите предпочитаемый пол для поиска:");
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    List<InlineKeyboardButton> rowInline = new ArrayList<>();
    rowInline.add(createButton("Мужской", "gender_pref_male"));
    rowInline.add(createButton("Женский", "gender_pref_female"));
    rowsInline.add(rowInline);

    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Любой", "gender_pref_any"));
    rowsInline.add(row2);

    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);
    executeMessage(message);
  }

  /**
   * Запрашивает геолокацию у пользователя
   */
  private void requestLiveLocation(Long chatId) {
    SendMessage message = new SendMessage(chatId.toString(),
        "Отправьте свою геолокацию, чтобы вас могли найти:");
    executeMessage(message);
  }

  /**
   * Показывает текущего пользователя из списка найденных
   */
  private void showCurrentNearbyUser(Long chatId) {
    List<User> nearbyUsers = nearbyUsersCache.get(chatId);
    Integer currentIndex = currentUserIndexCache.get(chatId);

    if (nearbyUsers == null || nearbyUsers.isEmpty()) {
      sendTextMessage(chatId,
          "😔 На данный момент никого поблизости не найдено, попробуйте позже.\n\n" +
              "📍 У вас активна геолокация на " + userLiveLocationDurations.getOrDefault(chatId, 0) +
              " часов. Если кто-то окажется рядом, мы вам сообщим!");
      return;
    }

    if (currentIndex == null || currentIndex < 0 || currentIndex >= nearbyUsers.size()) {
      currentIndex = 0;
      currentUserIndexCache.put(chatId, currentIndex);
    }

    User profile = nearbyUsers.get(currentIndex);

    // Если username == null, показываем заглушку
    String displayName = getDisplayName(profile);

    // Отправляем информацию о профиле
    SendMessage message = new SendMessage();
    message.setChatId(chatId);

    // Включаем информацию профиля, если доступна
    StringBuilder profileInfo = new StringBuilder();
    profileInfo.append("✨ @").append(displayName).append(" рядом!");

    if (profile.getAge() != null) {
      profileInfo.append("\n\n🎂 Возраст: ").append(profile.getAge());
    }

    if (profile.getGender() != null && !profile.getGender().isEmpty()) {
      profileInfo.append("\n⚧ Пол: ").append(profile.getGenderDisplay());
    }

    if (profile.getDescription() != null && !profile.getDescription().isEmpty()) {
      profileInfo.append("\n\n📝 О себе: ").append(profile.getDescription());
    }

    if (profile.getInterests() != null && !profile.getInterests().isEmpty()) {
      profileInfo.append("\n\n⭐ Интересы: ").append(profile.getInterests());
    }

    // Добавляем счетчик профилей
    profileInfo.append("\n\n🔢 Профиль ").append(currentIndex + 1).append(" из ").append(nearbyUsers.size());

    message.setText(profileInfo.toString());

    // Добавляем кнопки навигации и запроса на встречу
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    // Первый ряд кнопок - навигация
    List<InlineKeyboardButton> navigationRow = new ArrayList<>();

    if (nearbyUsers.size() > 1) {
      InlineKeyboardButton prevButton = new InlineKeyboardButton();
      prevButton.setText("⬅️ Предыдущий");
      prevButton.setCallbackData("prev_user");
      navigationRow.add(prevButton);

      InlineKeyboardButton nextButton = new InlineKeyboardButton();
      nextButton.setText("Следующий ➡️");
      nextButton.setCallbackData("next_user");
      navigationRow.add(nextButton);

      rowsInline.add(navigationRow);
    }

    // Второй ряд кнопок - отправка запроса
    List<InlineKeyboardButton> actionRow = new ArrayList<>();

    InlineKeyboardButton sendRequestButton = new InlineKeyboardButton();
    sendRequestButton.setText("📩 Отправить запрос");
    sendRequestButton.setCallbackData("send_request_" + profile.getTelegramId());

    actionRow.add(sendRequestButton);
    rowsInline.add(actionRow);

    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);

    executeMessage(message);

    // Если у пользователя есть фото, отправляем его отдельно
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

  /**
   * Показывает следующего пользователя из списка найденных
   */
  private void showNextUser(Long chatId, Integer messageId) {
    // Удаляем предыдущее сообщение
    deleteMessage(chatId, messageId);

    List<User> nearbyUsers = nearbyUsersCache.get(chatId);
    Integer currentIndex = currentUserIndexCache.get(chatId);

    if (nearbyUsers == null || nearbyUsers.isEmpty() || currentIndex == null) {
      sendTextMessage(chatId, "⚠️ Список пользователей не найден. Пожалуйста, обновите геолокацию.");
      return;
    }

    // Переходим к следующему пользователю или возвращаемся к началу
    currentIndex = (currentIndex + 1) % nearbyUsers.size();
    currentUserIndexCache.put(chatId, currentIndex);

    // Показываем нового пользователя
    showCurrentNearbyUser(chatId);
  }

  /**
   * Показывает предыдущего пользователя из списка найденных
   */
  private void showPreviousUser(Long chatId, Integer messageId) {
    // Удаляем предыдущее сообщение
    deleteMessage(chatId, messageId);

    List<User> nearbyUsers = nearbyUsersCache.get(chatId);
    Integer currentIndex = currentUserIndexCache.get(chatId);

    if (nearbyUsers == null || nearbyUsers.isEmpty() || currentIndex == null) {
      sendTextMessage(chatId, "⚠️ Список пользователей не найден. Пожалуйста, обновите геолокацию.");
      return;
    }

    // Переходим к предыдущему пользователю или к последнему в списке
    currentIndex = (currentIndex - 1 + nearbyUsers.size()) % nearbyUsers.size();
    currentUserIndexCache.put(chatId, currentIndex);

    // Показываем нового пользователя
    showCurrentNearbyUser(chatId);
  }

  /**
   * Уведомляет пользователя о запросе на встречу
   */
  private void notifyUserAboutMeetingRequest(Long receiverId, Long senderId) {
    User sender = userService.getUserByTelegramId(senderId);
    String senderName = getDisplayName(sender);

    // Получаем сам запрос на встречу
    List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
    MeetingRequest request = null;

    for (MeetingRequest req : requests) {
      if (req.getSender().getTelegramId().equals(senderId)) {
        request = req;
        break;
      }
    }

    if (request == null) {
      // Если запрос не найден, отправляем базовое уведомление
      sendTextMessage(receiverId, "✨ " + senderName + " отправил вам запрос на встречу!");
      return;
    }

    // Создаем сообщение с информацией о профиле
    StringBuilder requestInfo = new StringBuilder();
    requestInfo.append("✨ ").append(senderName).append(" отправил вам запрос на встречу!");

    if (sender.getAge() != null) {
      requestInfo.append("\n\n🎂 Возраст: ").append(sender.getAge());
    }

    if (sender.getGender() != null && !sender.getGender().isEmpty()) {
      requestInfo.append("\n⚧ Пол: ").append(sender.getGenderDisplay());
    }

    if (sender.getDescription() != null && !sender.getDescription().isEmpty()) {
      requestInfo.append("\n\n📝 О себе: ").append(sender.getDescription());
    }

    if (sender.getInterests() != null && !sender.getInterests().isEmpty()) {
      requestInfo.append("\n\n⭐ Интересы: ").append(sender.getInterests());
    }

    // Добавляем сообщение из запроса на встречу
    requestInfo.append("\n\n💬 Сообщение: ").append(request.getMessage());

    // Добавляем кнопки для принятия/отклонения запроса
    SendMessage message = new SendMessage();
    message.setChatId(receiverId.toString());
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

    // Если у отправителя есть фото профиля, отправляем его отдельно
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

    // Если в запросе есть фото, отправляем его отдельно
    if (request.hasPhoto()) {
      SendPhoto photoMessage = new SendPhoto();
      photoMessage.setChatId(receiverId.toString());
      photoMessage.setPhoto(new InputFile(request.getPhotoFileId()));
      photoMessage.setCaption("📸 Фото к запросу на встречу");

      try {
        execute(photoMessage);
      } catch (TelegramApiException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Отправляет текстовое сообщение
   */
  private void sendTextMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    executeMessage(message);
  }

  /**
   * Создает кнопку для инлайн-клавиатуры
   */
  private InlineKeyboardButton createButton(String text, String callbackData) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(text);
    button.setCallbackData(callbackData);
    return button;
  }

  /**
   * Удаляет сообщение
   */
  private void deleteMessage(Long chatId, Integer messageId) {
    try {
      execute(new DeleteMessage(chatId.toString(), messageId));
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  /**
   * Отправляет сообщение
   */
  private void executeMessage(SendMessage message) {
    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  /**
   * Получает отображаемое имя пользователя
   */
  private String getDisplayName(User user) {
    // Приоритет отображения: username -> firstName -> "Анонимный пользователь"
    if (user.getUsername() != null && !user.getUsername().isEmpty()) {
      return user.getUsername();
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

  /**
   * Получает текстовое представление пола для отображения
   */
  private String getGenderDisplay(String gender) {
    if (gender == null) return "Не указан";
    return switch (gender) {
      case "male" -> "Мужской";
      case "female" -> "Женский";
      case "other" -> "Другой";
      default -> "Не указан";
    };
  }

  /**
   * Получает текстовое представление предпочтений по полу для отображения
   */
  private String getGenderPreferenceDisplay(String genderPref) {
    if (genderPref == null) return "Любой";
    return switch (genderPref) {
      case "male" -> "Мужской";
      case "female" -> "Женский";
      case "any" -> "Любой";
      default -> "Любой";
    };
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