package com.sparta.whereismyparcel.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class PostFilter implements GlobalFilter {

	private static final Logger log = LoggerFactory.getLogger(PostFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return chain.filter(exchange)
			.then(Mono.fromRunnable(() -> log.info(
				"[GatewayResponse] path={}, status={}",
				exchange.getRequest().getPath(),
				exchange.getResponse().getStatusCode()
			)));
	}
}
