import { Body, Controller, Get, Post, Req, UseGuards } from '@nestjs/common'
import { getTelegramUser } from '../../common/get-telegram-user'
import { AuthGuard } from '../auth/auth.guard'
import { SwipesService } from './swipes.service'
import { SwipeDto } from './types/swipe.dto'

type TelegramRequest = Parameters<typeof getTelegramUser>[0]

@Controller('swipes')
@UseGuards(AuthGuard)
export class SwipesController {
  constructor (private readonly swipesService: SwipesService) {}

  @Get('queue')
  async getQueue (@Req() request: TelegramRequest) {
    return this.swipesService.fetchQueue(getTelegramUser(request))
  }

  @Post()
  async swipe (@Req() request: TelegramRequest, @Body() body: SwipeDto) {
    return this.swipesService.swipe(getTelegramUser(request), body)
  }
}
