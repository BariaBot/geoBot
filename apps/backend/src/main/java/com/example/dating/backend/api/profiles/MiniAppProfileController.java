package com.example.dating.backend.api.profiles;

import com.example.dating.backend.profile.ProfileResponse;
import com.example.dating.backend.profile.ProfileService;
import com.example.dating.backend.profile.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class MiniAppProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> fetchMyProfile(@RequestHeader("x-telegram-user-id") Long telegramUserId) {
        return ResponseEntity.ok(profileService.fetchProfile(telegramUserId));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(
        @RequestHeader("x-telegram-user-id") Long telegramUserId,
        @Valid @RequestBody ProfileUpdateRequest request
    ) {
        UpdateProfileRequest domainRequest = UpdateProfileRequest.builder()
            .displayName(request.displayName())
            .bio(request.bio())
            .gender(request.gender())
            .birthday(request.birthday())
            .city(request.city())
            .interests(request.interests())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .build();

        return ResponseEntity.ok(profileService.upsertProfile(telegramUserId, domainRequest));
    }
}
