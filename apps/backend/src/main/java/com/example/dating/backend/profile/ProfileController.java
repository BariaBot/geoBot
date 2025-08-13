package com.example.dating.backend.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;

    @GetMapping("/me")
    public ProfileDto me(Authentication authentication) {
        Long telegramId = Long.parseLong(authentication.getName());
        return ProfileDto.from(service.getOrCreate(telegramId));
        }

    @PutMapping
    public ProfileDto update(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        Long telegramId = Long.parseLong(authentication.getName());
        return ProfileDto.from(service.update(telegramId, request.bio(), request.city(), request.gender(), request.birthday()));
    }

    public record ProfileDto(Long userId, String bio, String city, Profile.Gender gender, LocalDate birthday) {
        static ProfileDto from(Profile p) {
            return new ProfileDto(p.getUserId(), p.getBio(), p.getCity(), p.getGender(), p.getBirthday());
        }
    }

    public record UpdateProfileRequest(String bio, String city, Profile.Gender gender, LocalDate birthday) {}
}
