package com.example.dating.backend.user;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User ensureUserExists(Long telegramId, String username) {
        return userRepository
                .findByTelegramId(telegramId)
                .map(existing -> updateUsernameIfNeeded(existing, username))
                .orElseGet(() -> createUser(telegramId, username));
    }

    private User updateUsernameIfNeeded(User user, String username) {
        if (username != null && !username.isBlank() && !username.equals(user.getUsername())) {
            user.setUsername(username);
            return userRepository.save(user);
        }
        return user;
    }

    private User createUser(Long telegramId, String username) {
        return userRepository.save(User.builder()
                .telegramId(telegramId)
                .username(username)
                .createdAt(OffsetDateTime.now())
                .build());
    }
}
