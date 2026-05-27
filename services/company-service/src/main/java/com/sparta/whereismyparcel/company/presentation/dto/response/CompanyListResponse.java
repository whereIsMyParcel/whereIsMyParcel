package com.sparta.whereismyparcel.company.presentation.dto.response;

import com.sparta.whereismyparcel.company.domain.entity.Company;
import com.sparta.whereismyparcel.company.domain.entity.CompanyType;

import java.util.UUID;

public record CompanyListResponse(
        UUID companyId,
        UUID hubId,
        CompanyType companyType,
        String companyName,
        String address
) {
    public static CompanyListResponse from(Company company) {
        return new CompanyListResponse(
                company.getId(),
                company.getHubId(),
                company.getCompanyType(),
                company.getCompanyName(),
                company.getAddress()
        );
    }
}
