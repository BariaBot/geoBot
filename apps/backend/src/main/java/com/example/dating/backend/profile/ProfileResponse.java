package com.example.dating.backend.profile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record ProfileResponse(
    Long telegramId,
    String displayName,
    String bio,
    String gender,
    LocalDate birthday,
    String city,
    boolean vip,
    OffsetDateTime vipUntil,
    LocationPayload location,
    List<String> interests
) {

  @Builder
  public record LocationPayload(Double latitude, Double longitude, OffsetDateTime updatedAt) {}
}
