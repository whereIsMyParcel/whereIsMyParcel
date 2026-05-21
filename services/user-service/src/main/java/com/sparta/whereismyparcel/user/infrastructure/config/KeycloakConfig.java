package com.sparta.whereismyparcel.user.infrastructure.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

	@Value("${keycloak.server-url}")
	private String serverUrl;

	@Value("${keycloak.realm}")
	private String realm;

	@Value("${keycloak.admin.client-id}")
	private String adminClientId;

	@Value("${keycloak.admin.client-secret}")
	private String adminClientSecret;

	@Bean
	public Keycloak keycloakAdmin() {
		return KeycloakBuilder.builder()
				.serverUrl(serverUrl)
				.realm(realm)
				.clientId(adminClientId)
				.clientSecret(adminClientSecret)
				.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
				.build();
	}
}
