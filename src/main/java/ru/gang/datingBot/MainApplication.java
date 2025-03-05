package ru.gang.datingBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;

import ru.gang.datingBot.bot.DatingBot;
import ru.gang.datingBot.bot.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.handler.MeetingPlaceHandler;
import ru.gang.datingBot.repository.ChatMessageRepository;
import ru.gang.datingBot.repository.MeetingRepository;
import ru.gang.datingBot.repository.UserRepository;
import ru.gang.datingBot.service.ChatService;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.PlaceService;
import ru.gang.datingBot.service.UserService;

@SpringBootApplication
@EnableScheduling
public class MainApplication {
  public static void main(String[] args) {
    SpringApplication.run(MainApplication.class, args);
  }

  @Bean
  public UserStateManager userStateManager() {
    return new UserStateManager();
  }

  @Bean
  public KeyboardService keyboardService() {
    return new KeyboardService();
  }

  @Bean
  public ApplicationRunner initBot(DatingBot datingBot) {
    return args -> {
      try {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(datingBot);
        System.out.println("Бот успешно запущен!");
      } catch (Exception e) {
        e.printStackTrace();
      }
    };
  }

  @Bean
  public ChatService chatService(
          ChatMessageRepository chatMessageRepository,
          MeetingRepository meetingRepository,
          UserRepository userRepository) {
    return new ChatService(chatMessageRepository, meetingRepository, userRepository);
  }

  @Bean
  public MessageSender messageSender(@Lazy DatingBot datingBot) {
    return new MessageSender(datingBot);
  }

  @Bean
  public MeetingPlaceHandler meetingPlaceHandler(
          UserService userService,
          MeetingService meetingService,
          PlaceService placeService,
          UserStateManager userStateManager,
          KeyboardService keyboardService,
          MessageSender messageSender) {
    return new MeetingPlaceHandler(
            userService,
            meetingService,
            placeService,
            userStateManager,
            keyboardService,
            messageSender);
  }

  @Bean
  public DatingBot datingBot(
          UserService userService,
          @Lazy MeetingService meetingService,
          ChatService chatService,
          @Lazy MeetingPlaceHandler meetingPlaceHandler) {
    
    // Создаем DatingBot с необходимыми зависимостями
    return new DatingBot(
            userService,
            meetingService,
            chatService,
            meetingPlaceHandler);
  }
}