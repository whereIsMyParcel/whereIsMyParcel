package com.sparta.whereismyparcel.gateway.filter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

/**
 * JWT 검증 후 claims를 X-User-* 헤더로 변환하여 하위 서비스에 전달하는 필터.
 * 클라이언트가 임의로 심은 X-User-* 헤더는 제거하여 스푸핑을 방지한다.
 */
@Component
public class JwtClaimsInjectionFilter implements GlobalFilter, Ordered {

	// Keycloak이 모든 사용자에게 자동 부여하는 내장 role — 도메인 권한이 아니므로 헤더 주입 대상에서 제외
	private static final Set<String> KEYCLOAK_SYSTEM_ROLES = Set.of(
		"offline_access", "uma_authorization"
	);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return ReactiveSecurityContextHolder.getContext()
			.map(ctx -> ctx.getAuthentication())
			.filter(auth -> auth instanceof JwtAuthenticationToken)
			.cast(JwtAuthenticationToken.class)
			.flatMap(auth -> {
				var jwt = auth.getToken();
				String userId = jwt.getSubject();
				String role = extractRole(jwt.getClaimAsMap("realm_access"));
				String username = jwt.getClaimAsString("preferred_username");

				ServerWebExchange mutated = exchange.mutate()
					.request(r -> r.headers(headers -> {
						removeUserHeaders(headers);
						if (userId != null) headers.set("X-User-Id", userId);
						if (role != null) headers.set("X-User-Role", role);
						if (username != null) headers.set("X-Username", username);
					}))
					.build();
				return chain.filter(mutated);
			})
			.switchIfEmpty(
				chain.filter(exchange.mutate()
					.request(r -> r.headers(this::removeUserHeaders))
					.build())
			);
	}

	@Override
	public int getOrder() {
		// Spring Security는 WebFilter(-100)로 먼저 실행되어 SecurityContext를 채운다.
		// 이 필터는 GlobalFilter이므로 Security 이후에 실행되며 HIGHEST_PRECEDENCE로 다른 GlobalFilter보다 먼저 헤더를 주입한다.
		return Ordered.HIGHEST_PRECEDENCE;
	}

	private void removeUserHeaders(org.springframework.http.HttpHeaders headers) {
		headers.remove("X-User-Id");
		headers.remove("X-User-Role");
		headers.remove("X-Username");
	}

	@SuppressWarnings("unchecked")
	private String extractRole(Map<String, Object> realmAccess) {
		if (realmAccess == null) return null;
		List<String> roles = (List<String>) realmAccess.get("roles");
		if (roles == null) return null;
		return roles.stream()
			.filter(r -> !r.startsWith("default-roles-") && !KEYCLOAK_SYSTEM_ROLES.contains(r))
			.findFirst()
			.orElse(null);
	}
}
