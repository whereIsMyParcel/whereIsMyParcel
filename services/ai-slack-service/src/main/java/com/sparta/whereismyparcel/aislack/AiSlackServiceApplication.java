package com.sparta.whereismyparcel.aislack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients; // EnableFeignClients 임포트

@SpringBootApplication
@EnableFeignClients(basePackages = "com.sparta.whereismyparcel.aislack.infrastructure.client") // Feign 클라이언트 스캔 활성화
public class AiSlackServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiSlackServiceApplication.class, args);
	}
}
