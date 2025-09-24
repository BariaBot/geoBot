import { plainToInstance } from 'class-transformer'
import {
  IsEnum,
  IsInt,
  IsOptional,
  IsUrl,
  Max,
  Min,
  validateSync
} from 'class-validator'

enum Environment {
  Development = 'development',
  Production = 'production',
  Test = 'test'
}

class EnvironmentVariables {
  @IsEnum(Environment)
    NODE_ENV: Environment = Environment.Development

  @IsInt()
  @Min(1025)
  @Max(65535)
    PORT: number = 4000

  @IsUrl({ require_tld: false })
    CORE_BASE_URL!: string

  @IsOptional()
  @IsUrl({ require_tld: false })
    TELEGRAM_BOT_API_URL?: string

  @IsOptional()
  @IsUrl({ require_tld: false })
    TON_WEBHOOK_URL?: string
}

export const validateEnv = (config: Record<string, unknown>) => {
  const validated = plainToInstance(EnvironmentVariables, config, {
    enableImplicitConversion: true
  })

  const validationErrors = validateSync(validated, { skipMissingProperties: false })

  if (validationErrors.length > 0) {
    const messages: string[] = []

    for (const error of validationErrors) {
      const constraints = error.constraints as Record<string, string> | undefined
      if (constraints !== undefined) {
        messages.push(...Object.values(constraints))
      }
    }

    throw new Error(messages.join(', '))
  }

  return validated
}
