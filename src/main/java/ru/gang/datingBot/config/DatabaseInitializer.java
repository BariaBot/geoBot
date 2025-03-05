package ru.gang.datingBot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.UserRepository;

import java.time.LocalDateTime;

@Component
public class DatabaseInitializer {

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void initializeDatabase() {
        System.out.println("Инициализация базы данных...");

        // Проверяем, пуста ли таблица users
        if (userRepository.count() == 0) {
            System.out.println("База пуста, создаю тестового пользователя");

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
            System.out.println("Тестовый пользователь создан с ID: " + testUser.getId());
        } else {
            System.out.println("В базе уже есть записи, пропускаю инициализацию");
        }
    }
}