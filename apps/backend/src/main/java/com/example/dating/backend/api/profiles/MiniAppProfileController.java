package com.example.dating.backend.api.profiles;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles")
public class MiniAppProfileController {

    private final MiniAppProfileService profileService;

    public MiniAppProfileController(MiniAppProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> fetchMyProfile(@RequestHeader("x-telegram-user-id") Long telegramUserId) {
        return ResponseEntity.ok(profileService.fetchProfile(telegramUserId));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(
        @RequestHeader("x-telegram-user-id") Long telegramUserId,
        @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(profileService.updateProfile(telegramUserId, request));
    }
}
