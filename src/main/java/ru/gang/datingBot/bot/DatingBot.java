package ru.gang.datingBot.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gang.datingBot.handler.CallbackQueryHandler;
import ru.gang.datingBot.handler.LocationHandler;
import ru.gang.datingBot.handler.MessageHandler;
import ru.gang.datingBot.handler.PhotoHandler;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.UserService;

@Component
public class DatingBot extends TelegramLongPollingBot {

  @Value("${bot.username}")
  private String botUsername;

  @Value("${bot.token}")
  private String botToken;

  private final MessageHandler messageHandler;
  private final CallbackQueryHandler callbackQueryHandler;
  private final LocationHandler locationHandler;
  private final PhotoHandler photoHandler;

  public DatingBot(UserService userService, MeetingService meetingService) {
    // Инициализируем обработчики и сервисы
    UserStateManager userStateManager = new UserStateManager();
    KeyboardService keyboardService = new KeyboardService();
    ProfileService profileService = new ProfileService(userService, keyboardService);
    MessageSender messageSender = new MessageSender(this);

    this.callbackQueryHandler = new CallbackQueryHandler(
        userService,
        meetingService,
        userStateManager,
        keyboardService,
        profileService,
        messageSender);

    this.messageHandler = new MessageHandler(
        userService,
        meetingService,
        userStateManager,
        keyboardService,
        profileService,
        messageSender);

    this.locationHandler = new LocationHandler(
        userService,
        userStateManager,
        messageSender);

    this.photoHandler = new PhotoHandler(
        userService,
        meetingService,
        userStateManager,
        messageSender);

    // Устанавливаем ссылку на CallbackQueryHandler для LocationHandler
    this.locationHandler.setCallbackQueryHandler(this.callbackQueryHandler);
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()) {
      var message = update.getMessage();
      Long chatId = message.getChatId();

      if (message.hasText()) {
        messageHandler.processTextMessage(chatId, message.getText());
      } else if (message.hasLocation()) {
        locationHandler.processLocationMessage(
            chatId,
            message.getLocation().getLatitude(),
            message.getLocation().getLongitude(),
            message.getMessageId(),
            update);
      } else if (message.hasPhoto()) {
        photoHandler.processPhotoMessage(chatId, message.getPhoto(), message.getMessageId());
      }
    }

    if (update.hasCallbackQuery()) {
      var callbackQuery = update.getCallbackQuery();
      Long chatId = callbackQuery.getMessage().getChatId();
      String data = callbackQuery.getData();
      Integer messageId = callbackQuery.getMessage().getMessageId();

      callbackQueryHandler.processCallbackQuery(chatId, data, messageId);
    }
  }

  @Override
  public String getBotUsername() {
    return botUsername;
  }

  @Override
  public String getBotToken() {
    return botToken;
  }
}