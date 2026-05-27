package com.sparta.whereismyparcel.company.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompanySearchHubRequest(
        @NotBlank
        String zipCode,

        @NotBlank
        String address,

        String addressDetails
) {
}
