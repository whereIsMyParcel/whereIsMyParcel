package com.sparta.whereismyparcel.company.presentation.dto.request;

import com.sparta.whereismyparcel.company.domain.entity.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CompanyRegisterRequest(

        @NotNull
        String hubId,

        @NotNull
        @Size(max = 30)
        CompanyType companyType,

        @NotBlank
        @Size(max = 100)
        String companyName,

        @NotBlank
        @Size(max = 30)
        String businessNumber,

        @NotBlank
        @Size(max = 50)
        String managerName,

        @NotBlank
        @Size(max = 30)
        String managerPhone,

        @NotBlank
        @Size(max = 20)
        String zipCode,

        @NotBlank
        @Size(max = 255)
        String address,

        @Size(max = 255)
        String addressDetail
) {
}
