package ru.gang.datingBot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class UserActivator {

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void activateAllUsers() {
        System.out.println("Активация всех пользователей...");
        List<User> users = userRepository.findAll();

        for (User user : users) {
            user.setActive(true);
            user.setDeactivateAt(LocalDateTime.now().plusDays(30)); // Активация на 30 дней
            userRepository.save(user);
            System.out.println("Активирован пользователь: " + user.getTelegramId() +
                    " (" + (user.getUsername() != null ? user.getUsername() : "без имени") + ")");
        }

        System.out.println("Всего активировано пользователей: " + users.size());
    }
}