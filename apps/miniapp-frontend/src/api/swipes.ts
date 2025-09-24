import { apiRequest } from './client';

export interface SwipeFeedItem {
  telegramId: number;
  displayName: string;
  bio: string | null;
  city: string | null;
  gender: string | null;
  distanceMeters: number | null;
  lastSeen: string | null;
  interests: string[];
}

export interface SwipeFeedResponse {
  timestamp: string;
  items: SwipeFeedItem[];
}

export interface SwipeResponse {
  match: boolean;
  targetTelegramId: number;
}

export async function fetchSwipeFeed(limit?: number): Promise<SwipeFeedResponse> {
  const searchParams = typeof limit === 'number' && Number.isInteger(limit) && limit > 0
    ? `?limit=${limit}`
    : '';

  return await apiRequest<SwipeFeedResponse>(`/swipes/feed${searchParams}`, {
    credentials: 'include'
  });
}

export async function sendSwipe(targetTelegramId: number): Promise<SwipeResponse> {
  return await apiRequest<SwipeResponse>('/swipes/like', {
    method: 'POST',
    credentials: 'include',
    body: JSON.stringify({ targetTelegramId })
  });
}
