package com.example.dating.backend.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record UpdateProfileRequest(
    @NotBlank(message = "display_name is required")
    @Size(min = 2, max = 64)
    String displayName,

    @Size(max = 1024)
    String bio,

    @Size(max = 20)
    String gender,

    LocalDate birthday,

    @Size(max = 255)
    String city,

    List<@Size(min = 1, max = 32) String> interests,

    Double latitude,

    Double longitude
) {}
