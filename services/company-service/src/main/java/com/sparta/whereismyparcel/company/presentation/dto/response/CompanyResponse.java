package com.sparta.whereismyparcel.company.presentation.dto.response;

import com.sparta.whereismyparcel.company.domain.entity.Company;
import com.sparta.whereismyparcel.company.domain.entity.CompanyType;

import java.util.UUID;

public record CompanyResponse(
        UUID companyId,
        UUID hubId,
        CompanyType companyType,
        String companyName,
        String businessNumber,
        String managerName,
        String managerPhone,
        String zipCode,
        String address,
        String addressDetail
) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getCompanyId(),
                company.getHubId(),
                company.getCompanyType(),
                company.getCompanyName(),
                company.getBusinessNumber(),
                company.getManagerName(),
                company.getManagerPhone(),
                company.getZipCode(),
                company.getAddress(),
                company.getAddressDetail()
        );
    }
}
