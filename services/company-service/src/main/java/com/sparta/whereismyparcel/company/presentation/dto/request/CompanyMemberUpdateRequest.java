package com.sparta.whereismyparcel.company.presentation.dto.request;

import com.sparta.whereismyparcel.company.domain.entity.CompanyRole;

import java.util.UUID;

public record CompanyMemberUpdateRequest(
        UUID companyId
) {
}
