import { IsInt, IsOptional, IsString, MaxLength } from 'class-validator'

export class MatchInviteDto {
  @IsOptional()
  @IsString()
  @MaxLength(120)
    matchId?: string | null

  @IsInt()
    targetTelegramId!: number

  @IsString()
  @MaxLength(80)
    targetName!: string
}

