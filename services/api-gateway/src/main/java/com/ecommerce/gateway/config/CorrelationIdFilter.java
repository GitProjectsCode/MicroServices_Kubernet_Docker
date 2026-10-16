package com.ecommerce.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * GlobalFilter that guarantees end-to-end distributed tracing by:
 * 1. Extracting or generating a unique X-Correlation-Id.
 * 2. Propagating the correlation ID downstream to target microservices.
 * 3. Appending the correlation ID to the HTTP response back to the client.
 * 4. Logging request routing and latency.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        // Retrieve existing correlation ID from request header or generate a new UUID
        String correlationId = headers.getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Store correlation ID in exchange attributes for downstream filters/controllers
        exchange.getAttributes().put(CORRELATION_ID_HEADER, correlationId);

        // Mutate request to ensure header is forwarded downstream to the target microservice
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        // Mutate response to attach header back to the calling client (Postman/browser)
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();
        String finalCorrelationId = correlationId;
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";

        log.info("[Gateway] Incoming request: {} {} [Correlation-ID: {}]", method, path, finalCorrelationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 500;
                    log.info("[Gateway] Completed request: {} {} -> HTTP {} in {}ms [Correlation-ID: {}]",
                            method, path, statusCode, duration, finalCorrelationId);
                }));
    }

    @Override
    public int getOrder() {
        // Run with highest precedence so the correlation ID is ready for all other filters
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
