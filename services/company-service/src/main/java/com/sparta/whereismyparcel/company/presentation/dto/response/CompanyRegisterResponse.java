//package com.sparta.whereismyparcel.company.presentation.dto.response;
//
//import com.sparta.whereismyparcel.company.domain.entity.Company;
//import com.sparta.whereismyparcel.company.domain.entity.CompanyType;
//
//import java.util.UUID;
//
//public record CompanyRegisterResponse(
//        UUID companyId,
//        UUID hubId,
//        CompanyType companyType,
//        String companyName,
//        String managerName,
//        String managerPhone,
//        String address,
//        String addressDetail
//) {
//    public static CompanyRegisterResponse from(Company company) {
//        return new CompanyRegisterResponse(
//                company.getCompanyId(),
//                company.getHubId(),
//                company.getCompanyType(),
//                company.getCompanyName(),
//                company.getManagerName(),
//                company.getManagerPhone(),
//                company.getAddress(),
//                company.getAddressDetail()
//        );
//    }
//}
