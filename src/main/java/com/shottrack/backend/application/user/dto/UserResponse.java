package com.shottrack.backend.application.user.dto;

import com.shottrack.backend.application.user.model.ExperienceLevel;
import com.shottrack.backend.application.user.model.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        ExperienceLevel experienceLevel,
        Role role,
        Instant createdAt
) {
}
