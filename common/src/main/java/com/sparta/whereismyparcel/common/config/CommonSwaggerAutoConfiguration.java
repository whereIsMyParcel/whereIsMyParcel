package com.sparta.whereismyparcel.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonSwaggerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(OpenAPI.class)
	public OpenAPI gatewayHeaderOpenAPI() {
		return new OpenAPI()
				.addSecurityItem(new SecurityRequirement()
						.addList("X-User-Id")
						.addList("X-User-Role")
						.addList("X-User-Status"))
				.components(new Components()
						.addSecuritySchemes("X-User-Id", apiKeyHeader("X-User-Id"))
						.addSecuritySchemes("X-User-Role", apiKeyHeader("X-User-Role"))
						.addSecuritySchemes("X-User-Status", apiKeyHeader("X-User-Status")));
	}

	private SecurityScheme apiKeyHeader(String name) {
		return new SecurityScheme()
				.name(name)
				.type(SecurityScheme.Type.APIKEY)
				.in(SecurityScheme.In.HEADER);
	}
}
