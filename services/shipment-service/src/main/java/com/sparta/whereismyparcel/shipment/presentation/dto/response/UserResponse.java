package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        String name,
        String email,
        String slackId,
        String businessNumber,
        UUID hubId,
        UUID companyId
) {
}
