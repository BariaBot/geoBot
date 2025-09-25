package com.example.dating.backend.api.profiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProfileResponse(
    Long telegramId,
    String name,
    String bio,
    List<String> interests,
    LocalDate birthday,
    ProfileLocation location,
    Instant updatedAt
) {}
