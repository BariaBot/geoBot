package com.example.dating.backend.api.swipes;

import jakarta.validation.constraints.NotNull;

public record SwipeRequest(
    Long targetTelegramId,
    @NotNull SwipeDirection direction
) {}
