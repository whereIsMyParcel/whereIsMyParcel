package com.sparta.whereismyparcel.company.presentation.dto.request;

import com.sparta.whereismyparcel.company.domain.entity.CompanyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CompanyRegisterRequest(

        @NotNull
        UUID hubId,

        @Schema(description = "업체 타입", example = "RECEIVER,SUPPLIER")
        @NotNull
        CompanyType companyType,

        @Schema(description = "업체 이름", example = "스파르타 물류")
        @NotBlank
        @Size(max = 100)
        String companyName,

        @Schema(description = "사업자등록번호", example = "123-45-67890")
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다. (예: 123-45-67890)")
        String businessNumber,

        @Schema(description = "사업자 명", example = "companyUser")
        @NotBlank
        @Size(max = 50)
        String managerName,

        @Schema(description = "사업자 전화번호", example = "000-1111-2222")
        @NotBlank
        @Size(max = 30)
        @Pattern(
                regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 02-123-4567 또는 010-1234-5678)"
        )
        String managerPhone,

        @Schema(description = "우편번호", example = "11111")
        @NotBlank
        @Size(max = 20)
        @Pattern(
                regexp = "^\\d{5}$",
                message = "우편번호는 숫자 5자리여야 합니다."
        )
        String zipCode,

        @Schema(description = "주소", example = "서울특별시 강남구")
        @NotBlank
        @Size(max = 255)
        String address,

        @Schema(description = "상세 주소", example = "스파르타빌딩 404호")
        @Size(max = 255)
        String addressDetail
) {
}
