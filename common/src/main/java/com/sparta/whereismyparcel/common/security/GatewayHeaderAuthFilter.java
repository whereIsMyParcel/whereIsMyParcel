package com.sparta.whereismyparcel.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String userId = request.getHeader("X-User-Id");
		String role = request.getHeader("X-User-Role");

		if (userId != null && role != null) {
			// Spring Security의 hasRole()은 ROLE_ 접두사를 기준으로 검사
			var auth = new UsernamePasswordAuthenticationToken(
					userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
			SecurityContextHolder.getContext().setAuthentication(auth);
		}

		filterChain.doFilter(request, response);
	}
}
