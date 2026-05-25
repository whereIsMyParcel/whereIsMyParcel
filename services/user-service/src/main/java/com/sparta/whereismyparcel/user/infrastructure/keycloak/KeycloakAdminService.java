package com.sparta.whereismyparcel.user.infrastructure.keycloak;

import com.sparta.whereismyparcel.user.domain.exception.KeycloakUserCreationFailedException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminService {

	private final Keycloak keycloak;

	@Value("${keycloak.realm}")
	private String realm;

	public UUID createUser(String username, String email, String password) {
		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(password);
		credential.setTemporary(false);

		UserRepresentation userRepresentation = new UserRepresentation();
		userRepresentation.setUsername(username);
		userRepresentation.setEmail(email);
		userRepresentation.setEnabled(false); // 승인 전 비활성
		userRepresentation.setEmailVerified(true);
		userRepresentation.setCredentials(List.of(credential));

		Response response = keycloak.realm(realm).users().create(userRepresentation);
		if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
			String errorMessage = response.readEntity(String.class);
			response.close();
			log.error("Keycloak 유저 생성 실패. status={}, reason={}", response.getStatus(), errorMessage);
			throw new KeycloakUserCreationFailedException();
		}
		// CreatedResponseUtil이 Location 헤더에서 ID를 파싱하고 response를 안전하게 닫음
		return UUID.fromString(CreatedResponseUtil.getCreatedId(response));
	}

	public void enableUser(UUID userId) {
		setEnabled(userId, true);
	}

	public void disableUser(UUID userId) {
		setEnabled(userId, false);
	}

	public void deleteUser(UUID userId) {
		keycloak.realm(realm).users().get(userId.toString()).remove();
	}

	private void setEnabled(UUID userId, boolean enabled) {
		var userResource = keycloak.realm(realm).users().get(userId.toString());
		UserRepresentation user = userResource.toRepresentation();
		user.setEnabled(enabled);
		userResource.update(user);
	}
}
