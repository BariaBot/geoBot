package ru.gang.datingBot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UserActivator {

    private static final Logger log = LoggerFactory.getLogger(UserActivator.class);

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void activateAllUsers() {
        log.info("Активация всех пользователей...");
        List<User> users = userRepository.findAll();

        for (User user : users) {
            user.setActive(true);
            user.setDeactivateAt(LocalDateTime.now().plusDays(30)); // Активация на 30 дней
            userRepository.save(user);
            log.info("Активирован пользователь: {} ({})", user.getTelegramId(),
                    (user.getUsername() != null ? user.getUsername() : "без имени"));
        }

        log.info("Всего активировано пользователей: {}", users.size());
    }
}