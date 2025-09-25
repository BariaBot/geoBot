package com.example.dating.backend.api.profiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class MiniAppProfileService {

    private final Map<Long, ProfileResponse> profiles = new ConcurrentHashMap<>();

    public MiniAppProfileService() {
        ProfileResponse seed = new ProfileResponse(
            111111L,
            "Алекс",
            "Люблю путешествия и вечерние прогулки",
            List.of("музыка", "спорт"),
            LocalDate.of(1995, 5, 12),
            new ProfileLocation("Москва", 55.751244, 37.618423),
            Instant.now()
        );
        profiles.put(seed.telegramId(), seed);
    }

    public ProfileResponse fetchProfile(Long telegramId) {
        return profiles.computeIfAbsent(telegramId, this::createBlankProfile);
    }

    public ProfileResponse updateProfile(Long telegramId, ProfileUpdateRequest request) {
        ProfileLocation location = buildLocation(request);
        ProfileResponse updated = new ProfileResponse(
            telegramId,
            request.displayName(),
            request.bio(),
            request.interests() == null ? List.of() : List.copyOf(request.interests()),
            request.birthday(),
            location,
            Instant.now()
        );
        profiles.put(telegramId, updated);
        return updated;
    }

    private ProfileResponse createBlankProfile(Long telegramId) {
        return new ProfileResponse(
            telegramId,
            "Новый пользователь",
            "",
            new ArrayList<>(),
            null,
            null,
            Instant.now()
        );
    }

    private ProfileLocation buildLocation(ProfileUpdateRequest request) {
        if (request.city() == null && request.latitude() == null && request.longitude() == null) {
            return null;
        }
        return new ProfileLocation(request.city(), request.latitude(), request.longitude());
    }
}
