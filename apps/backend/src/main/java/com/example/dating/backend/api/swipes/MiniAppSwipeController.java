package com.example.dating.backend.api.swipes;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/swipes")
public class MiniAppSwipeController {

    private final MiniAppSwipeService swipeService;

    public MiniAppSwipeController(MiniAppSwipeService swipeService) {
        this.swipeService = swipeService;
    }

    @GetMapping("/queue")
    public ResponseEntity<SwipeQueue> getQueue(@RequestHeader("x-telegram-user-id") Long telegramUserId) {
        return ResponseEntity.ok(swipeService.fetchQueue(telegramUserId));
    }

    @PostMapping
    public ResponseEntity<SwipeResponse> swipe(
        @RequestHeader("x-telegram-user-id") Long telegramUserId,
        @Valid @RequestBody SwipeRequest request
    ) {
        return ResponseEntity.ok(swipeService.processSwipe(telegramUserId, request));
    }
}
