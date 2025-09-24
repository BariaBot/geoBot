package com.example.dating.backend.api.swipes;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.dating.backend.api.profiles.ProfileLocation;
import com.example.dating.backend.api.profiles.ProfileResponse;

@Service
public class MiniAppSwipeService {

    private final Map<Long, Deque<SwipeQueueItem>> swipeQueues = new ConcurrentHashMap<>();

    public MiniAppSwipeService() {
        // seed queue for demo
        Deque<SwipeQueueItem> queue = new ArrayDeque<>();
        queue.add(new SwipeQueueItem(
            new ProfileResponse(
                222222L,
                "Катя",
                "Играю на пианино",
                List.of("музыка", "путешествия"),
                null,
                new ProfileLocation("Москва", 55.75222, 37.61556),
                Instant.now()
            ),
            1.2
        ));
        queue.add(new SwipeQueueItem(
            new ProfileResponse(
                333333L,
                "Сергей",
                "Бег и кофе",
                List.of("спорт"),
                null,
                new ProfileLocation("Москва", 55.76222, 37.62556),
                Instant.now()
            ),
            2.5
        ));
        swipeQueues.put(111111L, queue);
    }

    public SwipeQueue fetchQueue(Long telegramId) {
        Deque<SwipeQueueItem> queue = swipeQueues.computeIfAbsent(telegramId, key -> new ArrayDeque<>());
        return new SwipeQueue(new ArrayList<>(queue));
    }

    public SwipeResponse processSwipe(Long telegramId, SwipeRequest request) {
        Deque<SwipeQueueItem> queue = swipeQueues.computeIfAbsent(telegramId, key -> new ArrayDeque<>());
        queue.removeIf(item -> item.profile().telegramId().equals(request.targetTelegramId()));

        boolean matched = request.direction() != SwipeDirection.DISLIKE && request.targetTelegramId() % 2 == 0;
        return new SwipeResponse(matched, matched ? "match-" + telegramId + "-" + request.targetTelegramId() : null, Instant.now());
    }
}
