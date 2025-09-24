package com.example.dating.backend.api.profiles;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ProfileUpdateRequest(
    @NotBlank @Size(min = 2, max = 64) String displayName,
    @Size(max = 1024) String bio,
    @Size(max = 20) String gender,
    LocalDate birthday,
    @Size(max = 255) String city,
    List<@Size(max = 32) String> interests,
    Double latitude,
    Double longitude
) {}
