package com.sparta.whereismyparcel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.sparta.whereismyparcel")
@EnableFeignClients(basePackages = "com.sparta.whereismyparcel")
public class CompanyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompanyServiceApplication.class, args);
	}
}
