import { Injectable } from '@nestjs/common'
import { CoreHttpService } from '../../common/core-http.service'
import type { TelegramInitData } from '../auth/auth.service'
import { SwipeDto } from './types/swipe.dto'
import type { SwipeQueueDto } from './types/swipe.queue.response'
import type { SwipeResponseDto } from './types/swipe.response'

@Injectable()
export class SwipesService {
  constructor (private readonly coreHttp: CoreHttpService) {}

  async fetchQueue (user: TelegramInitData): Promise<SwipeQueueDto> {
    return await this.coreHttp.get<SwipeQueueDto>(
      '/swipes/queue',
      this.buildContext(user)
    )
  }

  async swipe (user: TelegramInitData, payload: SwipeDto): Promise<SwipeResponseDto> {
    return await this.coreHttp.post<SwipeResponseDto, SwipeDto>(
      '/swipes',
      this.buildContext(user),
      payload
    )
  }

  private buildContext (user: TelegramInitData) {
    return {
      telegramId: user.userId
    }
  }
}
