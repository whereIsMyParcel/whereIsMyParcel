package com.sparta.whereismyparcel.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class PreFilter implements GlobalFilter {

	private static final Logger log = LoggerFactory.getLogger(PreFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		log.info("[GatewayRequest] method={}, path={}", exchange.getRequest().getMethod(), exchange.getRequest().getPath());
		return chain.filter(exchange);
	}
}
