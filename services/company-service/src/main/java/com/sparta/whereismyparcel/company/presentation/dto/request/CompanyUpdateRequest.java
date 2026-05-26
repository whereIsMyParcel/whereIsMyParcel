package com.sparta.whereismyparcel.company.presentation.dto.request;

import com.sparta.whereismyparcel.company.domain.entity.CompanyStatus;
import com.sparta.whereismyparcel.company.domain.entity.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(

        @Schema(description = "업체 타입", example = "RECEIVER,SUPPLIER")
        @NotNull
        @Size(max = 30)
        CompanyType companyType,

        @Schema(description = "업체 이름", example = "(주)스파르타 물류")
        @NotBlank
        @Size(max = 100)
        String companyName,

        @Schema(description = "사업자 명", example = "companyOwner")
        @NotBlank
        @Size(max = 50)
        String managerName,

        @Schema(description = "사업자 전화번호", example = "010-1111-2222")
        @NotBlank
        @Size(max = 30)
        String managerPhone,

        @Schema(description = "우편번호", example = "00700")
        @NotBlank
        @Size(max = 20)
        String zipCode,

        @Schema(description = "주소", example = "서울특별시 강남구 개포동")
        @NotBlank
        @Size(max = 255)
        String address,

        @Schema(description = "상세 주소", example = "스파르타빌딩 704호")
        @NotBlank
        @Size(max = 255)
        String addressDetail,

        @Schema(description = "업체 상태", example = "ACTIVE")
        @NotNull
        @Size(max = 30)
        CompanyStatus status
) {
}
