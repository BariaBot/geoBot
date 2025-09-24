package com.example.dating.backend.swipe;

import lombok.Builder;

@Builder
public record SwipeDecisionResponse(
    boolean match,
    Long targetTelegramId
) {}
