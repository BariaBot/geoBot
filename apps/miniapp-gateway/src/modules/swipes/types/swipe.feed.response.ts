export interface SwipeFeedItem {
  telegramId: number
  displayName: string
  bio: string | null
  city: string | null
  gender: string | null
  distanceMeters: number | null
  lastSeen: string | null
  interests: string[]
}

export interface SwipeFeedResponse {
  timestamp: string
  items: SwipeFeedItem[]
}
