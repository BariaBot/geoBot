package ru.gang.datingBot.bot;

import java.time.LocalDateTime;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.springframework.stereotype.Component;
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
    switch (text) {
      case "/start":
        sendTimeSelection(chatId);
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

  private void processCallbackQuery(Long chatId, String data, Integer messageId) {
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

      sendTextMessage(chatId, "✅ Запрос на встречу отправлен!");
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
      System.out.println(" - " + user.getTelegramId() + " | " + user.getUsername() +
          " | lat: " + user.getLatitude() + " lon: " + user.getLongitude());
    }

    // Берем первого пользователя
    User profile = nearbyUsers.get(0);

    // Если username == null, показываем заглушку
    String displayName = (profile.getUsername() != null) ? profile.getUsername() : "Анонимный пользователь";

    System.out.println("Выбран пользователь: " + profile.getTelegramId() + " | " + displayName);

    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("✨ @" + displayName + " рядом!\nХотите отправить запрос?");

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

  @Override
  public String getBotUsername() {
    return "GeoGreet_bot";
  }

  @Override
  public String getBotToken() {
    return "7906499880:AAGXfaTwF3JXOsiYxIl_yvYdO696Po2DVOU";
  }
}
