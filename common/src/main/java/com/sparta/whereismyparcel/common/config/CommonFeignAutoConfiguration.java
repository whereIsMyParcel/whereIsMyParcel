package com.sparta.whereismyparcel.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.whereismyparcel.common.feign.CommonFeignErrorDecoder;
import com.sparta.whereismyparcel.common.feign.FeignHeaderPropagationInterceptor;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonFeignAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(FeignHeaderPropagationInterceptor.class)
	public FeignHeaderPropagationInterceptor feignHeaderPropagationInterceptor() {
		return new FeignHeaderPropagationInterceptor();
	}

	@Bean
	@ConditionalOnMissingBean(ErrorDecoder.class)
	public ErrorDecoder commonFeignErrorDecoder(ObjectMapper objectMapper) {
		return new CommonFeignErrorDecoder(objectMapper);
	}
}
