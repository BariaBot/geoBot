import { IsEnum, IsInt, ValidateIf } from 'class-validator'

enum SwipeDirection {
  Like = 'like',
  Dislike = 'dislike',
  Superlike = 'superlike',
  Undo = 'undo'
}

export class SwipeDto {
  @ValidateIf(o => o.direction !== SwipeDirection.Undo)
  @IsInt()
    targetTelegramId?: number

  @IsEnum(SwipeDirection)
    direction!: SwipeDirection
}

export { SwipeDirection }
