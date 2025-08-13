package com.example.dating.backend.profile;

import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profiles;
    private final UserRepository users;

    public Profile getOrCreate(Long telegramId) {
        User user = users.findByTelegramId(telegramId).orElseThrow();
        return profiles.findById(user.getId())
                .orElseGet(() -> profiles.save(Profile.builder()
                        .user(user)
                        .userId(user.getId())
                        .build()));
    }

    public Profile update(Long telegramId, String bio, String city, Profile.Gender gender, LocalDate birthday) {
        Profile profile = getOrCreate(telegramId);
        profile.setBio(bio);
        profile.setCity(city);
        profile.setGender(gender);
        profile.setBirthday(birthday);
        return profiles.save(profile);
    }
}
