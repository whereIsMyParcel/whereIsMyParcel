package com.sparta.whereismyparcel.aislack.infrastructure.client.dto;

import com.sparta.whereismyparcel.common.security.UserRole;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        String name,
        String email
) {
}
