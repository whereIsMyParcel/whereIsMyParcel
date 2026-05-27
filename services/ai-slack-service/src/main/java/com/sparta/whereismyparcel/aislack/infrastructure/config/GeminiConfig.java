package com.sparta.whereismyparcel.aislack.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper; // ObjectMapper 임포트
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GeminiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() { // ObjectMapper 빈 추가
        return new ObjectMapper();
    }
}
