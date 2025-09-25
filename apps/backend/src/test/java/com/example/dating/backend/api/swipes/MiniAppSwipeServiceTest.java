package com.example.dating.backend.api.swipes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MiniAppSwipeServiceTest {

    private MiniAppSwipeService service;

    @BeforeEach
    void setUp() {
        service = new MiniAppSwipeService();
    }

    @Test
    void fetchQueueReturnsSeededProfilesForKnownUser() {
        SwipeQueue queue = service.fetchQueue(111111L);

        assertNotNull(queue);
        assertEquals(3, queue.items().size());
        assertFalse(queue.undoAvailable());
        assertThat(queue.items())
                .extracting(item -> item.profile().telegramId())
                .containsExactly(222222L, 333333L, 444444L);
    }

    @Test
    void likeRemovesProfileFromQueueAndMarksMatchForEvenTelegramId() {
        SwipeResponse response = service.processSwipe(111111L, new SwipeRequest(222222L, SwipeDirection.LIKE));

        assertTrue(response.matched());
        assertNotNull(response.matchId());
        assertEquals(2, response.queue().items().size());
        assertTrue(response.queue().undoAvailable());
        assertThat(response.queue().items())
                .extracting(item -> item.profile().telegramId())
                .doesNotContain(222222L);
    }

    @Test
    void dislikeRemovesProfileButDoesNotMatch() {
        SwipeResponse response = service.processSwipe(111111L, new SwipeRequest(222222L, SwipeDirection.DISLIKE));

        assertFalse(response.matched());
        assertNull(response.matchId());
        assertEquals(2, response.queue().items().size());
        assertTrue(response.queue().undoAvailable());
        assertThat(response.queue().items())
                .extracting(item -> item.profile().telegramId())
                .doesNotContain(222222L);
    }

    @Test
    void undoRestoresLastRemovedProfileToFront() {
        service.processSwipe(111111L, new SwipeRequest(222222L, SwipeDirection.DISLIKE));
        SwipeResponse response = service.processSwipe(111111L, new SwipeRequest(null, SwipeDirection.UNDO));

        assertFalse(response.matched());
        List<SwipeQueueItem> items = response.queue().items();
        assertEquals(3, items.size());
        assertEquals(222222L, items.get(0).profile().telegramId());
        assertFalse(response.queue().undoAvailable());
    }

    @Test
    void undoWithoutHistoryKeepsQueueUntouched() {
        SwipeQueue before = service.fetchQueue(777777L);
        SwipeResponse response = service.processSwipe(777777L, new SwipeRequest(null, SwipeDirection.UNDO));

        assertFalse(response.matched());
        assertFalse(response.queue().undoAvailable());
        assertEquals(before.items().size(), response.queue().items().size());
        List<Long> expectedOrder = before.items().stream()
                .map(item -> item.profile().telegramId())
                .toList();
        assertThat(response.queue().items())
                .extracting(item -> item.profile().telegramId())
                .containsExactlyElementsOf(expectedOrder);
    }
}
