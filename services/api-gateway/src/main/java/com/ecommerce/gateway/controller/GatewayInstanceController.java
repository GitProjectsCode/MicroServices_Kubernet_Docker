package com.ecommerce.gateway.controller;

import com.ecommerce.gateway.config.CorrelationIdFilter;
import com.ecommerce.gateway.model.InstanceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Controller exposing instance identification and health information for the API Gateway.
 * Essential for testing Kubernetes load balancing and routing diagnostics.
 */
@RestController
public class GatewayInstanceController {

    private final String instanceId = UUID.randomUUID().toString();
    private final String hostname;

    public GatewayInstanceController() {
        // Read Kubernetes-assigned pod hostname or fallback to system hostname
        String host = System.getenv("HOSTNAME");
        if (host == null || host.trim().isEmpty()) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                host = "unknown-host-" + instanceId.substring(0, 8);
            }
        }
        this.hostname = host;
    }

    /**
     * GET /api/instance - Exposes instance metadata for load-balancing demonstration.
     */
    @GetMapping("/api/instance")
    public ResponseEntity<InstanceResponse> getInstance(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        InstanceResponse response = new InstanceResponse(
                "api-gateway",
                hostname,
                instanceId,
                Instant.now(),
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/gateway/health - Lightweight health probe endpoint.
     */
    @GetMapping("/api/gateway/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "api-gateway",
                "hostname", hostname,
                "timestamp", Instant.now().toString()
        ));
    }
}
