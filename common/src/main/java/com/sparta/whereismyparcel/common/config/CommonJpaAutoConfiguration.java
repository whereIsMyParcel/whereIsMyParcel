package com.sparta.whereismyparcel.common.config;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@AutoConfiguration
@ConditionalOnClass({EntityManager.class, EnableJpaAuditing.class})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class CommonJpaAutoConfiguration {

	@Bean
	public AuditorAware<String> auditorAware() {
		return () -> {
			ServletRequestAttributes attrs =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attrs == null) {
				return Optional.of("SYSTEM");
			}
			HttpServletRequest request = attrs.getRequest();
			String userId = request.getHeader("X-User-Id");
			if (!StringUtils.hasText(userId)) {
				return Optional.of("SYSTEM");
			}
			return Optional.of(userId);
		};
	}
}
