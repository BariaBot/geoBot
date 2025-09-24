export interface ProfileLocation {
  latitude: number
  longitude: number
  updatedAt: string
}

export interface ProfileResponse {
  telegramId: number
  displayName: string
  bio: string | null
  gender: string | null
  birthday: string | null
  city: string | null
  vip: boolean
  vipUntil: string | null
  location: ProfileLocation | null
  interests: string[]
}
