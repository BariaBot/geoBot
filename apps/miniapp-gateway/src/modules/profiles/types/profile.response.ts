export interface ProfileResponse {
  telegramId: number
  name: string
  bio: string
  interests: string[]
  birthday: string | null
  location: {
    city: string
    latitude: number
    longitude: number
  } | null
}
