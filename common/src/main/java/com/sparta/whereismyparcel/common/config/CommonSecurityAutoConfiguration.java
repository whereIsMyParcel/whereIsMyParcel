package com.sparta.whereismyparcel.common.config;

import com.sparta.whereismyparcel.common.security.GatewayHeaderAuthFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableMethodSecurity
public class CommonSecurityAutoConfiguration {

	// @ConditionalOnMissingBean을 클래스가 아닌 빈 단위에 적용한 이유:
	// @EnableWebSecurity·@EnableMethodSecurity는 서비스가 자체 FilterChain을 정의해도 항상 활성화되어야
	// @PreAuthorize가 동작한다. 기본 FilterChain 빈만 조건부로 등록한다.
	@Bean
	@ConditionalOnMissingBean(SecurityFilterChain.class)
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated()
				)
				.addFilterBefore(new GatewayHeaderAuthFilter(),
						UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
