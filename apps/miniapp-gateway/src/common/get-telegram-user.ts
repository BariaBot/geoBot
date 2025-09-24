import { UnauthorizedException } from '@nestjs/common'
import type { FastifyRequest } from 'fastify'
import type { TelegramInitData } from '../modules/auth/auth.service'

export const getTelegramUser = (request: FastifyRequest): TelegramInitData => {
  if (request.user === undefined) {
    throw new UnauthorizedException('Telegram init data is missing')
  }

  return request.user
}
