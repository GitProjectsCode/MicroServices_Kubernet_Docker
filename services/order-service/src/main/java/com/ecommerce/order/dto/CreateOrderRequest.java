package com.ecommerce.order.dto;

import java.math.BigDecimal;

/**
 * DTO for incoming order creation request.
 */
public record CreateOrderRequest(
        Long productId,
        Integer quantity
) {}
