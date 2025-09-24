import {
  IsArray,
  IsDateString,
  IsOptional,
  IsString,
  Length,
  MaxLength
} from 'class-validator'

export class UpdateProfileDto {
  @IsString()
  @Length(1, 80)
    name!: string

  @IsOptional()
  @IsString()
  @MaxLength(512)
    bio?: string

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
    interests?: string[]

  @IsOptional()
  @IsDateString()
    birthday?: string

  @IsOptional()
    location?: {
    city?: string
    latitude: number
    longitude: number
  }
}
