package com.tinder.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TrackingFilter implements GlobalFilter, Ordered {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
		if (correlationId == null) {
			correlationId = UUID.randomUUID().toString();
		}

		exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

		ServerHttpRequest request = exchange.getRequest().mutate().header(CORRELATION_ID_HEADER, correlationId).build();

		return chain.filter(exchange.mutate().request(request).build());
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
