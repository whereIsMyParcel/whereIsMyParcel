package com.sparta.whereismyparcel.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignHeaderPropagationInterceptor implements RequestInterceptor {

	private static final List<String> PROPAGATED_HEADERS = List.of(
			"X-User-Id", "X-Username", "X-User-Role", "X-User-Status"
	);

	@Override
	public void apply(RequestTemplate template) {
		ServletRequestAttributes attrs =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		// 스케줄러·비동기 스레드 등 HTTP 요청 컨텍스트가 없는 경우 전파 생략
		if (attrs == null) return;

		HttpServletRequest request = attrs.getRequest();
		for (String header : PROPAGATED_HEADERS) {
			String value = request.getHeader(header);
			if (StringUtils.hasText(value)) {
				template.header(header, value);
			}
		}
	}
}
