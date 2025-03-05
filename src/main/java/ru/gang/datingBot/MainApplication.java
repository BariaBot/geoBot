package ru.gang.datingBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.gang.datingBot.bot.DatingBot;
import ru.gang.datingBot.repository.ChatMessageRepository;
import ru.gang.datingBot.repository.MeetingRepository;
import ru.gang.datingBot.repository.UserRepository;
import ru.gang.datingBot.service.ChatService;

@SpringBootApplication
@EnableScheduling
public class MainApplication {
  public static void main(String[] args) {
    SpringApplication.run(MainApplication.class, args);
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
  public ChatService chatService(ChatMessageRepository chatMessageRepository, 
                             MeetingRepository meetingRepository,
                             UserRepository userRepository) {
    return new ChatService(chatMessageRepository, meetingRepository, userRepository);
  }
}