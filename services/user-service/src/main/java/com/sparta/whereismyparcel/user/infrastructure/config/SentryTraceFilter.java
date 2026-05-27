package com.sparta.whereismyparcel.user.infrastructure.config;

import io.sentry.Sentry;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Order(2)
public class SentryTraceFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String traceId = MDC.get("traceId");
        String userId = MDC.get("userId");
        String username = MDC.get("username");

        if (traceId != null) Sentry.setTag("traceId", traceId);
        if (userId != null) Sentry.setTag("userId", userId);
        if (username != null) Sentry.setTag("username", username);

        chain.doFilter(request, response);
    }
}
