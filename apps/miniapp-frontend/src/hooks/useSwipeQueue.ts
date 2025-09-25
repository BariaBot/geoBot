import { useCallback, useEffect, useState } from 'react';
import {
  fetchSwipeQueue,
  sendSwipeDecision,
  type SwipeQueueItemDto,
  type SwipeDirection,
} from '../api/swipes';
import { trackEvent } from '../utils/analytics';

interface SwipeQueueState {
  items: SwipeQueueItemDto[];
  loading: boolean;
  error?: string;
  undoAvailable: boolean;
  refresh: () => Promise<void>;
  swipeLike: (item: SwipeQueueItemDto) => Promise<void>;
  swipeDislike: (item: SwipeQueueItemDto) => Promise<void>;
  undo: () => Promise<void>;
}

export function useSwipeQueue(enabled = true): SwipeQueueState {
  const [items, setItems] = useState<SwipeQueueItemDto[]>([]);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState<string>();
  const [undoAvailable, setUndoAvailable] = useState(false);

  const loadQueue = useCallback(async () => {
    if (!enabled) return;
    setLoading(true);
    setError(undefined);
    try {
      const queue = await fetchSwipeQueue();
      setItems(queue.items);
      setUndoAvailable(queue.undoAvailable);
    } catch (err) {
      console.error('Failed to load swipe queue', err);
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [enabled]);

  useEffect(() => {
    if (enabled) {
      void loadQueue();
    }
  }, [enabled, loadQueue]);

  const submitSwipe = useCallback(async (direction: SwipeDirection, target?: number) => {
    setError(undefined);
    setLoading(true);
    try {
      const response = await sendSwipeDecision(direction, target);
      setItems(response.queue.items);
      setUndoAvailable(response.queue.undoAvailable);

      switch (direction) {
        case 'like':
        case 'superlike':
          trackEvent('swipe_like', { target });
          if (response.matched && typeof target === 'number') {
            trackEvent('swipe_match', { target });
          }
          break;
        case 'dislike':
          trackEvent('swipe_dislike', { target });
          break;
        case 'undo':
          trackEvent('swipe_undo');
          break;
        default:
          break;
      }
    } catch (err) {
      console.error('Swipe decision failed', err);
      setError(err instanceof Error ? err.message : 'Unknown error');
      await loadQueue();
    } finally {
      setLoading(false);
    }
  }, [loadQueue]);

  const swipeLike = useCallback(async (item: SwipeQueueItemDto) => {
    await submitSwipe('like', item.profile.telegramId);
  }, [submitSwipe]);

  const swipeDislike = useCallback(async (item: SwipeQueueItemDto) => {
    await submitSwipe('dislike', item.profile.telegramId);
  }, [submitSwipe]);

  const undo = useCallback(async () => {
    await submitSwipe('undo');
  }, [submitSwipe]);

  return {
    items,
    loading,
    error,
    undoAvailable,
    refresh: loadQueue,
    swipeLike,
    swipeDislike,
    undo,
  };
}
