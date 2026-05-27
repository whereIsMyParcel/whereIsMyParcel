package com.sparta.whereismyparcel.company.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompanyMemberRequest(
        @NotNull
        UUID companyMemberId
) {
}
