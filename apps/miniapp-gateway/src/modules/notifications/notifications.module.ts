import { Module } from '@nestjs/common'
import { AuthModule } from '../auth/auth.module'
import { NotificationsController } from './notifications.controller'
import { NotificationsService } from './notifications.service'
import { CoreHttpService } from '../../common/core-http.service'

@Module({
  imports: [AuthModule],
  controllers: [NotificationsController],
  providers: [NotificationsService, CoreHttpService]
})
export class NotificationsModule {}

