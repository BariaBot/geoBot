import { Injectable, Logger } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'
import { fetch, type Response } from 'undici'

@Injectable()
export class CoreHttpService {
  private readonly logger = new Logger(CoreHttpService.name)

  private readonly baseUrl: string

  constructor (private readonly configService: ConfigService) {
    this.baseUrl = this.configService.getOrThrow<string>('CORE_BASE_URL')
  }

  async get<T>(endpoint: string, initData: { telegramId: number }): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'GET',
      headers: this.buildHeaders(initData)
    })

    return await this.handleResponse<T>(response)
  }

  async put<T, B>(endpoint: string, initData: { telegramId: number }, body: B): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'PUT',
      headers: {
        ...this.buildHeaders(initData),
        'content-type': 'application/json'
      },
      body: JSON.stringify(body)
    })

    return await this.handleResponse<T>(response)
  }

  async post<T, B>(endpoint: string, initData: { telegramId: number }, body: B): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'POST',
      headers: {
        ...this.buildHeaders(initData),
        'content-type': 'application/json'
      },
      body: JSON.stringify(body)
    })

    return await this.handleResponse<T>(response)
  }

  async postWebhook<T, B>(endpoint: string, body: B): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'POST',
      headers: {
        'content-type': 'application/json'
      },
      body: JSON.stringify(body)
    })

    return await this.handleResponse<T>(response)
  }

  private buildHeaders (initData: { telegramId: number }) {
    return {
      'x-telegram-user-id': String(initData.telegramId)
    }
  }

  private async handleResponse<T>(response: Response): Promise<T> {
    if (response.ok !== true) {
      const message = await response.text()
      this.logger.error(`Core request failed ${response.status}: ${message}`)
      throw new Error(`Core request failed with status ${response.status}`)
    }

    return await (response.json() as Promise<T>)
  }
}
