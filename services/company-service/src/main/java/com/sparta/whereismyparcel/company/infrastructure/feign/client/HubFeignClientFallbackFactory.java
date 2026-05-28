package com.sparta.whereismyparcel.company.infrastructure.feign.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HubFeignClientFallbackFactory implements FallbackFactory<HubFeignClient> {

    @Override
    public HubFeignClient create(Throwable cause) {
        log.warn("[CircuitBreaker] hub-service 호출 실패", cause);
        return hubId -> {
            throw new ServiceUnavailableException();
        };
    }
}
