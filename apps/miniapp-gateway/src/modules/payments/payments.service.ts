import { Injectable } from '@nestjs/common'
import { CoreHttpService } from '../../common/core-http.service'
import type { TelegramInitData } from '../auth/auth.service'
import type { StarsPurchaseDto } from './types/stars-purchase.dto'
import type { StarPaymentIntentResponse } from './types/stars-purchase.response'

@Injectable()
export class PaymentsService {
  constructor (private readonly coreHttp: CoreHttpService) {}

  async createStarsPurchaseIntent (
    user: TelegramInitData,
    payload: StarsPurchaseDto
  ): Promise<StarPaymentIntentResponse> {
    return await this.coreHttp.post<StarPaymentIntentResponse, StarsPurchaseDto>(
      '/payments/stars/intents',
      this.buildContext(user),
      payload
    )
  }

  async handleStarsWebhook (body: unknown): Promise<{ ok: boolean }> {
    return await this.coreHttp.postWebhook<{ ok: boolean }, unknown>('/payments/stars/webhook', body)
  }

  private buildContext (user: TelegramInitData) {
    return {
      telegramId: user.userId
    }
  }
}
