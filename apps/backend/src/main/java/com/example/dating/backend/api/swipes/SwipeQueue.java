package com.example.dating.backend.api.swipes;

import java.util.List;

public record SwipeQueue(List<SwipeQueueItem> items, boolean undoAvailable) {}
