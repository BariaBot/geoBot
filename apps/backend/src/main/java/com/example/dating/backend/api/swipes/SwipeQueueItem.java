package com.example.dating.backend.api.swipes;

import com.example.dating.backend.api.profiles.ProfileResponse;

public record SwipeQueueItem(
    ProfileResponse profile,
    Double distanceKm
) {}
