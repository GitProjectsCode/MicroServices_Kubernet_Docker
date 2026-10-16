package com.ecommerce.product.model;

import java.time.Instant;

/**
 * Metadata record for identifying which replica pod served the request.
 */
public record InstanceResponse(
        String service,
        String hostname,
        String instanceId,
        Instant timestamp,
        String requestId
) {}
