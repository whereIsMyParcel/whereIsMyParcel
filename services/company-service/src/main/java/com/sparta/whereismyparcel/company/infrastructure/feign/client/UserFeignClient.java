package com.sparta.whereismyparcel.company.infrastructure.feign.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.UserIdResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    /**
     * [업체 등록 시] 사업자 번호로 유저 ID 조회
     * 사업자 번호를 전달 후 해당 사업자 번호를 가진 유저의 아이디를 받음
     */
    @GetMapping("/internal/v1/users/by-business-number/{businessNumber}")
    ApiResponse<UserIdResponse> getUserIdByBusinessNumber(@PathVariable String businessNumber);

    /**
     * [업체 등록 / 멤버 등록 시] 유저의 소속 컴퍼니 ID 업데이트 요청
     */
    @PatchMapping("/internal/v1/users/{userId}/companies/{companyId}")
    ApiResponse<Void> updateUserCompanyId(
            @PathVariable UUID userId,
            @PathVariable UUID companyId
    );

    /**
     * [직원 단건 삭제 시] 특정 직원의 유저 정보 삭제 (또는 소속 해제) 요청
     */
    @DeleteMapping("/internal/v1/users/{userId}")
    ApiResponse<Void> deleteUserOrClearCompany(@PathVariable UUID userId);

    /**
     * [업체 전체 삭제 시] 해당 업체를 바라보는 모든 유저 일괄 연쇄 삭제 요청
     */
    @DeleteMapping("/internal/v1/users/companies/{companyId}")
    ApiResponse<Void> deleteAllUsersInCompany(@PathVariable UUID companyId);
}
