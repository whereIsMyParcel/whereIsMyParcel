package com.sparta.whereismyparcel.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonSwaggerAutoConfiguration {

	@Value("${app.swagger.server-url:}")
	private String swaggerServerUrl;

	@Bean
	@ConditionalOnMissingBean(OpenAPI.class)
	public OpenAPI gatewayHeaderOpenAPI() {
		return new OpenAPI()
				.addSecurityItem(new SecurityRequirement()
						.addList("X-User-Id")
						.addList("X-User-Role")
						.addList("X-User-Status")
						.addList("X-Username"))
				.components(new Components()
						.addSecuritySchemes("X-User-Id", apiKeyHeader("X-User-Id"))
						.addSecuritySchemes("X-User-Role", apiKeyHeader("X-User-Role"))
						.addSecuritySchemes("X-User-Status", apiKeyHeader("X-User-Status"))
						.addSecuritySchemes("X-Username", apiKeyHeader("X-Username")));
	}

	// 게이트웨이를 통해 접근할 때 Swagger "Try it out"이 올바른 서버로 요청하도록 servers URL을 재정의
	@Bean
	public OpenApiCustomizer swaggerServerUrlCustomizer() {
		return openApi -> {
			if (swaggerServerUrl != null && !swaggerServerUrl.isBlank()) {
				openApi.setServers(List.of(new Server().url(swaggerServerUrl)));
			}
		};
	}

	private SecurityScheme apiKeyHeader(String name) {
		return new SecurityScheme()
				.name(name)
				.type(SecurityScheme.Type.APIKEY)
				.in(SecurityScheme.In.HEADER);
	}
}
