package com.ecommerce.gateway.model;

import java.time.Instant;

/**
 * Data Transfer Object representing the runtime metadata of a microservice instance.
 * Used for demonstrating Kubernetes load balancing across pods.
 */
public record InstanceResponse(
        String service,
        String hostname,
        String instanceId,
        Instant timestamp,
        String requestId
) {}
