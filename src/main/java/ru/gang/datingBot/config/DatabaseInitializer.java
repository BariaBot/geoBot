package ru.gang.datingBot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.UserRepository;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void initializeDatabase() {
        log.info("Инициализация базы данных...");

        // Проверяем, пуста ли таблица users
        if (userRepository.count() == 0) {
            log.info("База пуста, создаю тестового пользователя");

            User testUser = new User();
            testUser.setTelegramId(123456789L);
            testUser.setUsername("test_user");
            testUser.setFirstName("Тест");
            testUser.setLastName("Пользователь");
            testUser.setActive(true);
            testUser.setDeactivateAt(LocalDateTime.now().plusDays(30));
            testUser.setLastActive(LocalDateTime.now());
            testUser.setProfileCompleted(false);
            testUser.setSearchRadius(5);

            userRepository.save(testUser);
            log.info("Тестовый пользователь создан с ID: {}", testUser.getId());
        } else {
            log.info("В базе уже есть записи, пропускаю инициализацию");
        }
    }
}