package com.sparta.whereismyparcel.aislack.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GeminiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
