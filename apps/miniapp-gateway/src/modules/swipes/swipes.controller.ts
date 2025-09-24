import { Body, Controller, Get, Post, Query, Req, UseGuards } from '@nestjs/common'
import { getTelegramUser } from '../../common/get-telegram-user'
import { AuthGuard } from '../auth/auth.guard'
import { SwipesService } from './swipes.service'
import { SwipeDto } from './types/swipe.dto'

type TelegramRequest = Parameters<typeof getTelegramUser>[0]

@Controller('swipes')
@UseGuards(AuthGuard)
export class SwipesController {
  constructor (private readonly swipesService: SwipesService) {}

  @Get('feed')
  async getFeed (@Req() request: TelegramRequest, @Query('limit') limit?: string) {
    const parsedLimit = typeof limit === 'string' ? Number(limit) : undefined
    const feedLimit = parsedLimit !== undefined && Number.isInteger(parsedLimit) && parsedLimit > 0
      ? parsedLimit
      : undefined

    return this.swipesService.fetchFeed(getTelegramUser(request), feedLimit)
  }

  @Post('decision')
  async swipe (@Req() request: TelegramRequest, @Body() body: SwipeDto) {
    return this.swipesService.swipe(getTelegramUser(request), body)
  }
}
