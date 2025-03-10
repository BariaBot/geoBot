package ru.gang.datingBot.bot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gang.datingBot.handler.CallbackQueryHandler;
import ru.gang.datingBot.handler.ChatHandler;
import ru.gang.datingBot.handler.LocationHandler;
import ru.gang.datingBot.handler.MessageHandler;
import ru.gang.datingBot.handler.PhotoHandler;
import ru.gang.datingBot.service.*;

@Getter
@Setter
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
  private final ChatHandler chatHandler;
  private final KeyboardService keyboardService;
  private final UserStateManager userStateManager;

  public DatingBot(
          UserService userService, 
          MeetingService meetingService, 
          ChatService chatService,
          SubscriptionService subscriptionService) {
    this.userStateManager = new UserStateManager();
    this.keyboardService = new KeyboardService();
    ProfileService profileService = new ProfileService(userService, keyboardService);
    MessageSender messageSender = new MessageSender(this);

    this.callbackQueryHandler = new CallbackQueryHandler(
            userService,
            meetingService,
            userStateManager,
            keyboardService,
            profileService,
            messageSender,
            subscriptionService);

    this.chatHandler = new ChatHandler(
            userService, 
            meetingService, 
            chatService, 
            userStateManager, 
            messageSender);

    this.callbackQueryHandler.setChatHandler(this.chatHandler);

    this.messageHandler = new MessageHandler(
            userService,
            meetingService,
            userStateManager,
            keyboardService,
            profileService,
            messageSender,
            subscriptionService);

    this.locationHandler = new LocationHandler(
            userService,
            userStateManager,
            messageSender);

    this.photoHandler = new PhotoHandler(
            userService,
            meetingService,
            userStateManager,
            messageSender);

    this.locationHandler.setCallbackQueryHandler(this.callbackQueryHandler);
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage()) {
      var message = update.getMessage();
      Long chatId = message.getChatId();

      if (userStateManager.isUserInState(chatId, UserStateManager.UserState.CHATTING)) {
        if (message.hasText()) {
          String text = message.getText();
          
          if ("/end_chat".equals(text)) {
            chatHandler.endCurrentChat(chatId);
          } else {
            chatHandler.processChatMessage(chatId, text);
          }
        } else if (message.hasPhoto()) {
          chatHandler.processChatPhoto(chatId, message.getPhoto());
        } else if (message.hasSticker()) {
          chatHandler.processChatSticker(chatId, message.getSticker());
        } else if (message.hasAnimation()) {
          chatHandler.processChatAnimation(chatId, message.getAnimation());
        } else if (message.hasVideo()) {
          chatHandler.processChatVideo(chatId, message.getVideo());
        } else if (message.hasVoice()) {
          chatHandler.processChatVoice(chatId, message.getVoice());
        } else if (message.hasAudio()) {
          chatHandler.processChatAudio(chatId, message.getAudio());
        } else if (message.hasDocument()) {
          chatHandler.processChatDocument(chatId, message.getDocument());
        }
        return;
      }

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