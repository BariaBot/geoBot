package com.example.dating.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    public User getOrCreate(Long telegramId, String username) {
        return repository.findByTelegramId(telegramId)
                .orElseGet(() -> repository.save(User.builder()
                        .telegramId(telegramId)
                        .username(username)
                        .createdAt(OffsetDateTime.now())
                        .build()));
    }
}
