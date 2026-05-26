package com.sparta.whereismyparcel.common.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignRetryConfig {

    @Bean
    public Retryer feignRetryer() {
        // 💡 주기(period): 100ms 지연 후 시작
        // 💡 최대주기(maxPeriod): 재시도 간격은 최대 1초(1000ms)까지 늘어남
        // 💡 최대시도횟수(maxAttempts): 최초 호출 포함 총 3번 시도하고 안 되면 에러 터뜨림!
        return new Retryer.Default(100, 1000, 3);
    }
}
