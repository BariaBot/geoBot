import { useCallback, useEffect, useState } from 'react';
import { fetchSwipeFeed, sendSwipe, type SwipeFeedItem } from '../api/swipes';
import { trackEvent } from '../utils/analytics';

interface SwipeFeedState {
  items: SwipeFeedItem[];
  loading: boolean;
  error?: string;
  refresh: () => Promise<void>;
  swipeLike: (item: SwipeFeedItem) => Promise<void>;
  skip: () => void;
}

export function useSwipeFeed(enabled = true): SwipeFeedState {
  const [items, setItems] = useState<SwipeFeedItem[]>([]);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState<string>();

  const loadFeed = useCallback(async () => {
    if (!enabled) return;
    setLoading(true);
    setError(undefined);
    try {
      const { items: feed } = await fetchSwipeFeed(20);
      setItems(feed);
    } catch (err) {
      console.error('Failed to load swipe feed', err);
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [enabled]);

  useEffect(() => {
    if (enabled) {
      void loadFeed();
    }
  }, [enabled, loadFeed]);

  const swipeLike = useCallback(async (item: SwipeFeedItem) => {
    if (!enabled) return;
    try {
      setItems((prev) => prev.filter((candidate) => candidate.telegramId !== item.telegramId));
      const response = await sendSwipe(item.telegramId);
      trackEvent('swipe_like', { target: item.telegramId });
      if (response.match) {
        trackEvent('swipe_match', { target: item.telegramId });
      }
    } catch (err) {
      console.error('Swipe like failed', err);
      setError(err instanceof Error ? err.message : 'Unknown error');
      await loadFeed();
    }
  }, [enabled, loadFeed]);

  const skip = useCallback(() => {
    setItems((prev) => prev.slice(1));
  }, []);

  return {
    items,
    loading,
    error,
    refresh: loadFeed,
    swipeLike,
    skip
  };
}
