import { Injectable } from '@nestjs/common'
import { CoreHttpService } from '../../common/core-http.service'
import type { TelegramInitData } from '../auth/auth.service'
import type { UpdateProfileDto } from './types/update-profile.dto'
import type { ProfileResponse } from './types/profile.response'

@Injectable()
export class ProfilesService {
  constructor (private readonly coreHttp: CoreHttpService) {}

  async fetchProfile (user: TelegramInitData): Promise<ProfileResponse> {
    return await this.coreHttp.get<ProfileResponse>(
      '/profiles/me',
      this.buildContext(user)
    )
  }

  async updateProfile (user: TelegramInitData, payload: UpdateProfileDto): Promise<ProfileResponse> {
    return await this.coreHttp.put<ProfileResponse, UpdateProfileDto>(
      '/profiles/me',
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
