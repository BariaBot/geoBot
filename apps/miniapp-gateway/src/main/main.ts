import 'reflect-metadata'
import { Logger } from '@nestjs/common'
import { NestFactory } from '@nestjs/core'
import {
  FastifyAdapter,
  type NestFastifyApplication
} from '@nestjs/platform-fastify'
import fastifyHelmet from '@fastify/helmet'
import fastifyRateLimit from '@fastify/rate-limit'
import { AppModule } from '../modules/app.module'

const DEFAULT_PORT = Number(process.env.PORT ?? 4000)

async function bootstrap () {
  const fastifyAdapter = new FastifyAdapter({
    trustProxy: true
  })

  const app = await NestFactory.create<NestFastifyApplication>(AppModule, fastifyAdapter, {
    bufferLogs: true
  })

  await app.register(fastifyHelmet as any, { contentSecurityPolicy: false })
  await app.register(fastifyRateLimit as any, {
    max: 100,
    timeWindow: '1 minute'
  })

  app.setGlobalPrefix('api/v1')

  await app.listen({ port: DEFAULT_PORT, host: '0.0.0.0' })
  Logger.log(`Mini App Gateway running on port ${DEFAULT_PORT}`, 'Bootstrap')
}

bootstrap().catch((error) => {
  // eslint-disable-next-line no-console
  console.error('Failed to bootstrap gateway', error)
  process.exit(1)
})
