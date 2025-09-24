import { IsInt, IsString, Length, Min } from 'class-validator'

export class StarsPurchaseDto {
  @IsString()
  @Length(1, 64)
    productCode!: string

  @IsInt()
  @Min(1)
    quantity!: number
}
