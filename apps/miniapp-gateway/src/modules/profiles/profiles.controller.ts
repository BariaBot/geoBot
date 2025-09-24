import { Body, Controller, Get, Put, Req, UseGuards } from '@nestjs/common'
import { getTelegramUser } from '../../common/get-telegram-user'
import { AuthGuard } from '../auth/auth.guard'
import { ProfilesService } from './profiles.service'
import { UpdateProfileDto } from './types/update-profile.dto'

type TelegramRequest = Parameters<typeof getTelegramUser>[0]

@Controller('profiles')
@UseGuards(AuthGuard)
export class ProfilesController {
  constructor (private readonly profilesService: ProfilesService) {}

  @Get('me')
  async me (@Req() request: TelegramRequest) {
    return this.profilesService.fetchProfile(getTelegramUser(request))
  }

  @Put('me')
  async update (@Req() request: TelegramRequest, @Body() body: UpdateProfileDto) {
    return this.profilesService.updateProfile(getTelegramUser(request), body)
  }
}
