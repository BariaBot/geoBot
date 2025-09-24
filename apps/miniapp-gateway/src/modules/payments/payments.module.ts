import { Module } from '@nestjs/common'
import { ConfigModule } from '@nestjs/config'
import { AuthModule } from '../auth/auth.module'
import { PaymentsController } from './payments.controller'
import { PaymentsService } from './payments.service'
import { CoreHttpService } from '../../common/core-http.service'

@Module({
  imports: [ConfigModule, AuthModule],
  controllers: [PaymentsController],
  providers: [PaymentsService, CoreHttpService]
})
export class PaymentsModule {}
