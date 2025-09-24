import { Injectable } from '@nestjs/common'
import { CoreHttpService } from '../../common/core-http.service'
import type { TelegramInitData } from '../auth/auth.service'
import type { SwipeDto } from './types/swipe.dto'
import type { SwipeResponse } from './types/swipe.response'
import type { SwipeQueueResponse } from './types/swipe.queue.response'

@Injectable()
export class SwipesService {
  constructor (private readonly coreHttp: CoreHttpService) {}

  async swipe (user: TelegramInitData, payload: SwipeDto): Promise<SwipeResponse> {
    return await this.coreHttp.post<SwipeResponse, SwipeDto>(
      '/swipes',
      this.buildContext(user),
      payload
    )
  }

  async getQueue (user: TelegramInitData, limit?: number): Promise<SwipeQueueResponse> {
    const searchParams = typeof limit === 'number' ? `?limit=${limit}` : ''

    return await this.coreHttp.get<SwipeQueueResponse>(
      `/swipes/queue${searchParams}`,
      this.buildContext(user)
    )
  }

  private buildContext (user: TelegramInitData) {
    return {
      telegramId: user.userId
    }
  }
}
