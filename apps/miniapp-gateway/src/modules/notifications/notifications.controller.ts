import { Body, Controller, Post, Req, UseGuards } from '@nestjs/common'
import { getTelegramUser } from '../../common/get-telegram-user'
import { AuthGuard } from '../auth/auth.guard'
import { NotificationsService } from './notifications.service'
import { MatchInviteDto } from './types/match-invite.dto'

type TelegramRequest = Parameters<typeof getTelegramUser>[0]

@Controller('notifications')
@UseGuards(AuthGuard)
export class NotificationsController {
  constructor (private readonly notificationsService: NotificationsService) {}

  @Post('match')
  async sendMatchInvite (@Req() request: TelegramRequest, @Body() body: MatchInviteDto) {
    await this.notificationsService.sendMatchInvite(getTelegramUser(request), body)
    return { ok: true }
  }
}

