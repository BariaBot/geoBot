import { Injectable, Logger } from '@nestjs/common'
import type { TelegramInitData } from '../auth/auth.service'
import { CoreHttpService } from '../../common/core-http.service'
import { MatchInviteDto } from './types/match-invite.dto'

@Injectable()
export class NotificationsService {
  private readonly logger = new Logger(NotificationsService.name)

  constructor (private readonly coreHttp: CoreHttpService) {}

  async sendMatchInvite (user: TelegramInitData, payload: MatchInviteDto): Promise<void> {
    try {
      await this.coreHttp.postWebhook('/notifications/match', {
        initiatorTelegramId: user.userId,
        matchId: payload.matchId ?? null,
        targetTelegramId: payload.targetTelegramId,
        targetName: payload.targetName
      })
    } catch (error) {
      this.logger.warn('Match invite webhook failed', error)
    }
  }
}

