package com.sparta.whereismyparcel.common.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.whereismyparcel.common.exception.RemoteServiceException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonFeignErrorDecoder implements ErrorDecoder {

	private static final Logger log = LoggerFactory.getLogger(CommonFeignErrorDecoder.class);

	private final ObjectMapper objectMapper;

	public CommonFeignErrorDecoder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Exception decode(String methodKey, Response response) {
		try {
			if (response.body() == null) {
				return new RemoteServiceException(response.status(), "COMMON-998", "원격 서비스 호출 실패 (응답 본문 없음)");
			}
			String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
			ApiResponse<?> apiResponse = objectMapper.readValue(body, ApiResponse.class);
			return new RemoteServiceException(
					response.status(),
					apiResponse.errorCode(),
					apiResponse.message()
			);
		} catch (Exception e) {
			log.warn("Feign 오류 응답 파싱 실패. methodKey={}, status={}", methodKey, response.status(), e);
			return new RemoteServiceException(response.status(), "COMMON-999", "원격 서비스 호출에 실패했습니다.");
		}
	}
}
