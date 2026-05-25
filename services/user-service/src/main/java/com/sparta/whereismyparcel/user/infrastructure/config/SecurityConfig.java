package com.sparta.whereismyparcel.user.infrastructure.config;

import com.sparta.whereismyparcel.common.security.GatewayHeaderAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**", "/internal/**", "/actuator/**",
								"/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(new GatewayHeaderAuthFilter(),
						UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
