package com.example.dating.backend.api.profiles;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @NotBlank @Size(max = 80) String name,
    @Size(max = 512) String bio,
    List<@Size(max = 32) String> interests,
    LocalDate birthday,
    ProfileLocation location
) {}
