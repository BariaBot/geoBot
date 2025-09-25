package com.example.dating.backend.api.notifications;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MatchInviteRequest(
    @Size(max = 120) String matchId,
    @NotNull Long targetTelegramId,
    @NotBlank @Size(max = 80) String targetName
) {
}
