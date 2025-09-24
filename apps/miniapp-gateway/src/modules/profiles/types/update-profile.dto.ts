import {
  IsArray,
  IsDateString,
  IsNumber,
  IsOptional,
  IsString,
  Length,
  MaxLength
} from 'class-validator'

export class UpdateProfileDto {
  @IsString()
  @Length(2, 64)
    displayName!: string

  @IsOptional()
  @IsString()
  @MaxLength(1024)
    bio?: string

  @IsOptional()
  @IsString()
  @Length(1, 20)
    gender?: string

  @IsOptional()
  @IsDateString()
    birthday?: string

  @IsOptional()
  @IsString()
  @MaxLength(255)
    city?: string

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
    interests?: string[]

  @IsOptional()
  @IsNumber({ allowNaN: false, allowInfinity: false })
    latitude?: number

  @IsOptional()
  @IsNumber({ allowNaN: false, allowInfinity: false })
    longitude?: number
}
