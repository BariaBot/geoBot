import type { ProfileResponse } from '../../profiles/types/profile.response'

export interface SwipeQueueItem {
  profile: ProfileResponse
  distanceKm?: number | null
}

export interface SwipeQueueResponse {
  items: SwipeQueueItem[]
}
