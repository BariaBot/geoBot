package ru.gang.datingBot.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
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
    System.out.println("Получено обновление: " + update);

    // Обработка текстовых сообщений
    if (update.hasMessage() && update.getMessage().hasText()) {
      var message = update.getMessage();
      Long chatId = message.getChatId();
      String text = message.getText();

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

    // Обработка нажатий на inline-кнопки
    if (update.hasCallbackQuery()) {
      var callbackQuery = update.getCallbackQuery();
      String data = callbackQuery.getData();
      Long chatId = callbackQuery.getMessage().getChatId();

      System.out.println("Нажата кнопка: " + data);

      // Обработка выбора времени
      if (data.equals("1 час") || data.equals("3 часа") || data.equals("6 часов")) {
        int duration = Integer.parseInt(data.split(" ")[0]);
        userLiveLocationDurations.put(chatId, duration);
        sendRadiusSelection(chatId);
      }

      // Обработка выбора радиуса
      if (data.equals("1 км") || data.equals("3 км") || data.equals("5 км")) {
        int radius = Integer.parseInt(data.split(" ")[0]);
        userSearchRadius.put(chatId, radius);
        requestLiveLocation(chatId);
      }
    }
  }


  private void sendTimeSelection(Long chatId) {
    SendMessage message = new SendMessage(chatId.toString(), "Выберите, на сколько часов включить геолокацию:");
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
    rowsInline.add(rowInline);
    markupInline.setKeyboard(rowsInline);
    message.setReplyMarkup(markupInline);
    executeMessage(message);
  }

  private void requestLiveLocation(Long chatId) {
    SendMessage message = new SendMessage(chatId.toString(), "Отправьте свою геолокацию, чтобы вас могли найти:");
    executeMessage(message);
  }

  private void suggestNearbyUser(Long chatId, double lat, double lon, int radius) {
    List<User> nearbyUsers = userService.findNearbyUsers(lat, lon, radius);

    // Проверяем, что список не null и не пустой
    if (nearbyUsers != null && !nearbyUsers.isEmpty()) {
      User profile = nearbyUsers.get(0);
      SendMessage message = new SendMessage();
      message.setChatId(chatId);
      message.setText("✨ " + profile.getUsername() + " рядом!\nХотите отправить запрос?");

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
    } else {
      sendTextMessage(chatId, "😔 Пока рядом никого нет. Попробуйте позже!");
    }
  }

  private void sendTextMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    executeMessage(message);
  }



  private void sendMeetingRequestPrompt(Long chatId, Long receiverId) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("Введите сообщение для запроса встречи:");

    userPendingRequests.put(chatId, receiverId);
    executeMessage(message);
  }

  private InlineKeyboardButton createButton(String text, String callbackData) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(text);
    button.setCallbackData(callbackData);
    return button;
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
    return "GeoGreetBot";
  }

  @Override
  public String getBotToken() {
    return "6933686090:AAGCO0I-zEu00iKA-aCQ13GKYL0e-kgVDos";
  }
}
