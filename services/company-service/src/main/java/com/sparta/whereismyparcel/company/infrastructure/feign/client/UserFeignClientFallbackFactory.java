package com.sparta.whereismyparcel.company.infrastructure.feign.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.company.presentation.dto.response.UserIdResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.warn("[CircuitBreaker] user-service 호출 실패", cause);
        return new UserFeignClient() {
            @Override
            public ApiResponse<UserIdResponse> getUserIdByBusinessNumber(String businessNumber) {
                throw new ServiceUnavailableException();
            }

            @Override
            public ApiResponse<Void> updateUserCompanyId(UUID userId, UUID companyId) {
                throw new ServiceUnavailableException();
            }

            @Override
            public ApiResponse<Void> deleteUserOrClearCompany(UUID userId) {
                throw new ServiceUnavailableException();
            }

            @Override
            public ApiResponse<Void> deleteAllUsersInCompany(UUID companyId) {
                throw new ServiceUnavailableException();
            }
        };
    }
}
