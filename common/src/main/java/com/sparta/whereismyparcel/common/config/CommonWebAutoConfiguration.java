package com.sparta.whereismyparcel.common.config;

import com.sparta.whereismyparcel.common.exception.GlobalExceptionHandler;
import com.sparta.whereismyparcel.common.filter.MdcLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@AutoConfiguration
@ConditionalOnClass(RestControllerAdvice.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MdcLoggingFilter.class)
	public FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilter() {
		FilterRegistrationBean<MdcLoggingFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new MdcLoggingFilter());
		registration.setOrder(1);
		return registration;
	}

	@Bean
	@ConditionalOnMissingBean(GlobalExceptionHandler.class)
	@ConditionalOnProperty(
		prefix = "whereismyparcel.common.web.exception",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true
	)
	public GlobalExceptionHandler globalExceptionHandler() {
		return new GlobalExceptionHandler();
	}
}
