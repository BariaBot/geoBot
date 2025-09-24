import { Injectable } from '@nestjs/common'
import type { CanActivate, ExecutionContext } from '@nestjs/common'
import type { FastifyRequest } from 'fastify'
import { AuthService } from './auth.service'

const INIT_DATA_HEADER = 'x-telegram-init-data'

const isTelegramInitData = (value: unknown): value is string => typeof value === 'string' && value.trim().length > 0

@Injectable()
export class AuthGuard implements CanActivate {
  constructor (private readonly authService: AuthService) {}

  canActivate (context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<FastifyRequest>()

    const initDataCandidate = this.extractInitDataCandidate(request)

    if (!isTelegramInitData(initDataCandidate)) {
      return false
    }

    request.user = this.authService.validateInitData(initDataCandidate)
    return true
  }

  private extractInitDataCandidate (request: FastifyRequest): unknown {
    const headerValue = request.headers[INIT_DATA_HEADER]
    if (isTelegramInitData(headerValue)) {
      return headerValue
    }

    const queryValue = (request.query as Record<string, unknown> | undefined)?.initData
    if (isTelegramInitData(queryValue)) {
      return queryValue
    }

    return null
  }
}
