import { type TelegramInitData } from '../modules/auth/auth.service'

declare module 'fastify' {
  interface FastifyRequest {
    user?: TelegramInitData
  }
}
