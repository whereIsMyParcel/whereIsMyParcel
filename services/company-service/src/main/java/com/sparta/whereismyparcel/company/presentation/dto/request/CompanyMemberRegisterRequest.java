package com.sparta.whereismyparcel.company.presentation.dto.request;

import com.sparta.whereismyparcel.company.domain.entity.CompanyRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CompanyMemberRegisterRequest(
        @NotNull
        UUID companyId
) {
}
