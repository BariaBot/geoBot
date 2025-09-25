import { apiRequest } from './client';

export type SwipeDirection = 'like' | 'dislike' | 'superlike' | 'undo';

export interface ProfileLocationDto {
  cityName: string | null;
  latitude: number | null;
  longitude: number | null;
}

export interface SwipeProfileDto {
  telegramId: number;
  name: string;
  bio: string | null;
  interests: string[];
  birthday: string | null;
  location: ProfileLocationDto | null;
  updatedAt: string;
}

export interface SwipeQueueItemDto {
  profile: SwipeProfileDto;
  distanceKm: number | null;
}

export interface SwipeQueueDto {
  items: SwipeQueueItemDto[];
  undoAvailable: boolean;
}

export interface SwipeDecisionResponseDto {
  matched: boolean;
  matchId: string | null;
  createdAt: string;
  queue: SwipeQueueDto;
}

export function fetchSwipeQueue(): Promise<SwipeQueueDto> {
  return apiRequest<SwipeQueueDto>('/swipes/queue', {
    credentials: 'include',
  });
}

export function sendSwipeDecision(
  direction: SwipeDirection,
  targetTelegramId?: number,
): Promise<SwipeDecisionResponseDto> {
  return apiRequest<SwipeDecisionResponseDto>('/swipes', {
    method: 'POST',
    credentials: 'include',
    body: JSON.stringify({
      direction,
      targetTelegramId,
    }),
  });
}
