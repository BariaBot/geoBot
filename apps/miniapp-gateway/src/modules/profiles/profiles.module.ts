import { Module } from '@nestjs/common'
import { ConfigModule } from '@nestjs/config'
import { AuthModule } from '../auth/auth.module'
import { CoreHttpService } from '../../common/core-http.service'
import { ProfilesController } from './profiles.controller'
import { ProfilesService } from './profiles.service'

@Module({
  imports: [AuthModule, ConfigModule],
  controllers: [ProfilesController],
  providers: [ProfilesService, CoreHttpService]
})
export class ProfilesModule {}
