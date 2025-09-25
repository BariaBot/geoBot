import type { SwipeQueueDto } from './swipe.queue.response'

export interface SwipeResponseDto {
  matched: boolean
  matchId: string | null
  createdAt: string
  queue: SwipeQueueDto
}
