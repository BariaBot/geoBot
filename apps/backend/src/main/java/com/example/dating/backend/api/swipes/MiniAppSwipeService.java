package com.example.dating.backend.api.swipes;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.dating.backend.api.profiles.ProfileLocation;
import com.example.dating.backend.api.profiles.ProfileResponse;

@Service
public class MiniAppSwipeService {

    private static final int HISTORY_LIMIT = 10;

    private final Map<Long, SwipeSession> sessions = new ConcurrentHashMap<>();

    public MiniAppSwipeService() {
        sessions.put(111111L, createSession(111111L));
    }

    public SwipeQueue fetchQueue(Long telegramId) {
        SwipeSession session = sessions.computeIfAbsent(telegramId, this::createSession);
        synchronized (session) {
            return snapshotQueue(session);
        }
    }

    public SwipeResponse processSwipe(Long telegramId, SwipeRequest request) {
        SwipeSession session = sessions.computeIfAbsent(telegramId, this::createSession);
        synchronized (session) {
            Instant now = Instant.now();
            return switch (request.direction()) {
                case LIKE, SUPERLIKE -> handlePositiveSwipe(session, telegramId, request, now);
                case DISLIKE -> handleDislike(session, request, now);
                case UNDO -> handleUndo(session, now);
            };
        }
    }

    private SwipeResponse handlePositiveSwipe(SwipeSession session, Long telegramId, SwipeRequest request, Instant now) {
        Long targetId = requireTarget(request);
        SwipeQueueItem removed = removeFromQueue(session.queue(), targetId);
        if (removed != null) {
            session.history().push(new SwipeHistoryEntry(removed));
            trimHistory(session.history());
        }

        boolean matched = targetId % 2 == 0;
        String matchId = matched ? "match-" + telegramId + "-" + targetId : null;

        return new SwipeResponse(matched, matchId, now, snapshotQueue(session));
    }

    private SwipeResponse handleDislike(SwipeSession session, SwipeRequest request, Instant now) {
        Long targetId = requireTarget(request);
        SwipeQueueItem removed = removeFromQueue(session.queue(), targetId);
        if (removed != null) {
            session.history().push(new SwipeHistoryEntry(removed));
            trimHistory(session.history());
        }
        return new SwipeResponse(false, null, now, snapshotQueue(session));
    }

    private SwipeResponse handleUndo(SwipeSession session, Instant now) {
        SwipeHistoryEntry previous = session.history().pollFirst();
        if (previous != null) {
            session.queue().removeIf(item -> item.profile().telegramId().equals(previous.item().profile().telegramId()));
            session.queue().addFirst(previous.item());
        }
        return new SwipeResponse(false, null, now, snapshotQueue(session));
    }

    private SwipeQueue snapshotQueue(SwipeSession session) {
        return new SwipeQueue(new ArrayList<>(session.queue()), !session.history().isEmpty());
    }

    private SwipeQueueItem removeFromQueue(Deque<SwipeQueueItem> queue, Long targetTelegramId) {
        Iterator<SwipeQueueItem> iterator = queue.iterator();
        while (iterator.hasNext()) {
            SwipeQueueItem candidate = iterator.next();
            if (candidate.profile().telegramId().equals(targetTelegramId)) {
                iterator.remove();
                return candidate;
            }
        }
        return null;
    }

    private Long requireTarget(SwipeRequest request) {
        Long target = request.targetTelegramId();
        if (target == null) {
            throw new IllegalArgumentException("targetTelegramId is required for direction " + request.direction());
        }
        return target;
    }

    private SwipeSession createSession(Long ignoredTelegramId) {
        Deque<SwipeQueueItem> queue = new ArrayDeque<>(seedQueue());
        return new SwipeSession(queue, new ArrayDeque<>());
    }

    private List<SwipeQueueItem> seedQueue() {
        Instant now = Instant.now();
        return List.of(
            new SwipeQueueItem(
                new ProfileResponse(
                    222222L,
                    "Катя",
                    "Играю на пианино",
                    List.of("музыка", "путешествия"),
                    null,
                    new ProfileLocation("Москва", 55.75222, 37.61556),
                    now
                ),
                1.2
            ),
            new SwipeQueueItem(
                new ProfileResponse(
                    333333L,
                    "Сергей",
                    "Бег и кофе",
                    List.of("спорт"),
                    null,
                    new ProfileLocation("Москва", 55.76222, 37.62556),
                    now
                ),
                2.5
            ),
            new SwipeQueueItem(
                new ProfileResponse(
                    444444L,
                    "Маша",
                    "Маркетолог, изучаю UX",
                    List.of("маркетинг", "книги"),
                    null,
                    new ProfileLocation("Москва", 55.74222, 37.60556),
                    now
                ),
                0.8
            )
        );
    }

    private void trimHistory(Deque<SwipeHistoryEntry> history) {
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    private record SwipeSession(Deque<SwipeQueueItem> queue, Deque<SwipeHistoryEntry> history) {}

    private record SwipeHistoryEntry(SwipeQueueItem item) {}
}
