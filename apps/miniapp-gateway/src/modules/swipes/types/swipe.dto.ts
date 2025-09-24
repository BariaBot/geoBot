import { IsEnum, IsInt } from 'class-validator'

enum SwipeDirection {
  Like = 'like',
  Dislike = 'dislike',
  Superlike = 'superlike'
}

export class SwipeDto {
  @IsInt()
    targetTelegramId!: number

  @IsEnum(SwipeDirection)
    direction!: SwipeDirection
}

export { SwipeDirection }
