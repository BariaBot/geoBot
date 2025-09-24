import crypto from 'node:crypto'
import { Injectable, UnauthorizedException } from '@nestjs/common'

const FIVE_MINUTES_IN_MS = 5 * 60 * 1000

const isNonEmptyString = (value: unknown): value is string => typeof value === 'string' && value.trim().length > 0

@Injectable()
export class AuthService {
  validateInitData (initDataRaw: string): TelegramInitData {
    const params = new URLSearchParams(initDataRaw)
    const data: Record<string, string> = {}

    params.forEach((value, key) => {
      data[key] = value
    })

    const hash = data.hash
    if (!isNonEmptyString(hash)) {
      throw new UnauthorizedException('Missing initData hash')
    }

    const authDateRaw = data.auth_date
    const authDate = Number.parseInt(authDateRaw ?? '', 10)
    const authDateIsValid = Number.isInteger(authDate) && authDate > 0

    if (!authDateIsValid || Date.now() - authDate * 1000 > FIVE_MINUTES_IN_MS) {
      throw new UnauthorizedException('initData expired')
    }

    const botToken = process.env.TELEGRAM_BOT_TOKEN
    if (!isNonEmptyString(botToken)) {
      throw new UnauthorizedException('Telegram bot token is not configured')
    }

    const secret = crypto.createHash('sha256').update(botToken).digest()
    const dataCheckString = Object.entries(data)
      .filter(([key]) => key !== 'hash')
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([key, value]) => `${key}=${value}`)
      .join('\n')

    const hmac = crypto.createHmac('sha256', secret).update(dataCheckString).digest('hex')
    if (hmac !== hash) {
      throw new UnauthorizedException('initData hash mismatch')
    }

    const userPayload = this.parseUserPayload(data.user)
    const userIdCandidate = userPayload?.id ?? data.user_id
    const userId = typeof userIdCandidate === 'string' || typeof userIdCandidate === 'number'
      ? Number(userIdCandidate)
      : Number.NaN

    if (!Number.isInteger(userId) || userId <= 0) {
      throw new UnauthorizedException('initData missing user id')
    }

    return {
      hash,
      queryId: data.query_id,
      authDate,
      user: userPayload,
      userId
    }
  }

  private parseUserPayload (rawUser: string | undefined): Record<string, unknown> | undefined {
    if (!isNonEmptyString(rawUser)) {
      return undefined
    }

    try {
      const parsed = JSON.parse(rawUser) as Record<string, unknown>
      return parsed
    } catch (error) {
      throw new UnauthorizedException('initData user payload malformed', { cause: error as Error })
    }
  }
}

export interface TelegramInitData {
  queryId?: string
  user?: Record<string, unknown>
  authDate?: number
  userId: number
  hash: string
}
