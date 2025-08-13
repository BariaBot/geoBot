package com.example.dating.backend.match;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;

    @PostMapping("/like/{targetUserId}")
    public MatchResponse like(@PathVariable Long targetUserId, Authentication auth) {
        Long fromTelegramId = Long.valueOf(auth.getName());
        boolean matched = matchService.like(fromTelegramId, targetUserId);
        return new MatchResponse(matched);
    }

    @GetMapping("/matches")
    public List<Match> matches(Authentication auth) {
        Long telegramId = Long.valueOf(auth.getName());
        return matchService.getMatches(telegramId);
    }

    public record MatchResponse(boolean matched) {}
}
