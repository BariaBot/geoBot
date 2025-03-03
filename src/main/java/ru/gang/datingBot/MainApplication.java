package ru.gang.datingBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import ru.gang.datingBot.bot.DatingBot;

@SpringBootApplication
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
}
