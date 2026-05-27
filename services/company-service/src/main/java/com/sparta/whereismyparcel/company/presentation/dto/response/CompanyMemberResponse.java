package com.sparta.whereismyparcel.company.presentation.dto.response;

import com.sparta.whereismyparcel.company.domain.entity.CompanyMember;

import java.util.UUID;

public record CompanyMemberResponse(
        UUID companyMemberId,
        UUID userId,
        UUID companyId
) {
    public static CompanyMemberResponse from(CompanyMember companyMember) {
        return new CompanyMemberResponse(
                companyMember.getCompanyMemberId(),
                companyMember.getUserId(),
                companyMember.getCompany().getCompanyId()
        );
    }
}
