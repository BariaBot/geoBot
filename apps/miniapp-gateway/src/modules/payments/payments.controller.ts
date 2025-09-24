import { Body, Controller, Post, Req, UseGuards } from '@nestjs/common'
import { getTelegramUser } from '../../common/get-telegram-user'
import { AuthGuard } from '../auth/auth.guard'
import { PaymentsService } from './payments.service'
import { StarsPurchaseDto } from './types/stars-purchase.dto'

type TelegramRequest = Parameters<typeof getTelegramUser>[0]

@Controller('payments')
@UseGuards(AuthGuard)
export class PaymentsController {
  constructor (private readonly paymentsService: PaymentsService) {}

  @Post('stars/purchase-intent')
  async createStarsPurchaseIntent (@Req() request: TelegramRequest, @Body() body: StarsPurchaseDto) {
    return this.paymentsService.createStarsPurchaseIntent(getTelegramUser(request), body)
  }
}
