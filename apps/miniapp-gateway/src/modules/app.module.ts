import { Module } from '@nestjs/common'
import { ConfigModule } from '@nestjs/config'
import { validateEnv } from '../config/validate-env'
import { AuthModule } from './auth/auth.module'
import { ProfilesModule } from './profiles/profiles.module'
import { SwipesModule } from './swipes/swipes.module'
import { PaymentsModule } from './payments/payments.module'
import { NotificationsModule } from './notifications/notifications.module'

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      validate: validateEnv,
      cache: true
    }),
    AuthModule,
    ProfilesModule,
    SwipesModule,
    PaymentsModule,
    NotificationsModule
  ]
})
export class AppModule {}
