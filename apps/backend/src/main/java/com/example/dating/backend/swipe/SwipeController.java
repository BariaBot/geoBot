package com.example.dating.backend.swipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/swipes")
@RequiredArgsConstructor
public class SwipeController {

  private final SwipeService swipeService;

  @GetMapping("/feed")
  public SwipeFeedResponse feed(
      @RequestHeader("x-telegram-user-id") Long telegramUserId,
      @RequestParam(value = "limit", required = false) Integer limit
  ) {
    return swipeService.loadFeed(telegramUserId, limit);
  }

  @PostMapping("/like")
  @ResponseStatus(HttpStatus.OK)
  public SwipeDecisionResponse like(
      @RequestHeader("x-telegram-user-id") Long telegramUserId,
      @Valid @RequestBody SwipeLikeRequest request
  ) {
    return swipeService.like(telegramUserId, request.targetTelegramId());
  }

  public record SwipeLikeRequest(@NotNull Long targetTelegramId) {}
}
