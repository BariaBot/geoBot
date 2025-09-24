package com.example.dating.backend.swipe;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record SwipeFeedResponse(
    OffsetDateTime timestamp,
    List<SwipeFeedItem> items
) {}
