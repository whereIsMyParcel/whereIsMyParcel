package com.sparta.whereismyparcel.company.presentation.dto.response;

import java.util.UUID;

public record CompanySearchHubResponse(
        UUID hubId
) {
    public static CompanySearchHubResponse from(UUID hubId) {
        return new CompanySearchHubResponse(hubId);
    }
}
