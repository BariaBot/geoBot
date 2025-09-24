import { Injectable } from '@nestjs/common'
import { CoreHttpService } from '../../common/core-http.service'
import type { TelegramInitData } from '../auth/auth.service'
import { SwipeDirection, SwipeDto } from './types/swipe.dto'
import type { SwipeResponse } from './types/swipe.response'
import type { SwipeFeedResponse } from './types/swipe.feed.response'

@Injectable()
export class SwipesService {
  constructor (private readonly coreHttp: CoreHttpService) {}

  async fetchFeed (user: TelegramInitData, limit?: number): Promise<SwipeFeedResponse> {
    const searchParams = typeof limit === 'number' && Number.isInteger(limit) && limit > 0
      ? `?limit=${limit}`
      : ''

    return await this.coreHttp.get<SwipeFeedResponse>(
      `/swipes/feed${searchParams}`,
      this.buildContext(user)
    )
  }

  async swipe (user: TelegramInitData, payload: SwipeDto): Promise<SwipeResponse> {
    switch (payload.direction) {
      case SwipeDirection.Like:
      case SwipeDirection.Superlike:
        return await this.coreHttp.post<SwipeResponse, { targetTelegramId: number }>(
          '/swipes/like',
          this.buildContext(user),
          { targetTelegramId: payload.targetTelegramId }
        )
      case SwipeDirection.Dislike:
      default:
        // Dislike currently short-circuits client-side; backend has no state to persist.
        return Promise.resolve({ match: false, targetTelegramId: payload.targetTelegramId })
    }
  }

  private buildContext (user: TelegramInitData) {
    return {
      telegramId: user.userId
    }
  }
}
