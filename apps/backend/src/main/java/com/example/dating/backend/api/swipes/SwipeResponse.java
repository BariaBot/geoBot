package com.example.dating.backend.api.swipes;

import java.time.Instant;

public record SwipeResponse(
    boolean matched,
    String matchId,
    Instant createdAt,
    SwipeQueue queue
) {}
