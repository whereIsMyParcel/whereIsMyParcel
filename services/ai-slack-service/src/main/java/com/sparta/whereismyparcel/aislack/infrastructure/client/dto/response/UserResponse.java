package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response;

import java.util.UUID;

public record UserResponse(
        String userId,
        String username,
        String name,
        String email,
        String slackId
) {
}
