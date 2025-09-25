package com.example.dating.backend.profile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;

    @GetMapping("/me")
    public ProfileResponse me(Authentication authentication) {
        Long telegramId = Long.parseLong(authentication.getName());
        return service.fetchProfile(telegramId);
    }

    @PutMapping
    public ProfileResponse update(
        Authentication authentication,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long telegramId = Long.parseLong(authentication.getName());
        return service.upsertProfile(telegramId, request);
    }
}
