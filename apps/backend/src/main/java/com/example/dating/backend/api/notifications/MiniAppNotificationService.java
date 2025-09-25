package com.example.dating.backend.api.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MiniAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MiniAppNotificationService.class);

    public void sendMatchInvite(Long initiatorTelegramId, MatchInviteRequest request) {
        log.info(
            "[match-invite-stub] initiator={}, target={}, matchId={}",
            initiatorTelegramId,
            request.targetTelegramId(),
            request.matchId()
        );
    }
}
