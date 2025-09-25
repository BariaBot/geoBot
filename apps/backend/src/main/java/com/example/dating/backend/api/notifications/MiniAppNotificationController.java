package com.example.dating.backend.api.notifications;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class MiniAppNotificationController {

    private final MiniAppNotificationService notificationService;

    public MiniAppNotificationController(MiniAppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/match")
    public ResponseEntity<Map<String, Boolean>> sendMatchInvite(
        @RequestHeader("x-telegram-user-id") Long telegramUserId,
        @Valid @RequestBody MatchInviteRequest request
    ) {
        notificationService.sendMatchInvite(telegramUserId, request);
        return ResponseEntity.ok(Map.of("ok", Boolean.TRUE));
    }
}
