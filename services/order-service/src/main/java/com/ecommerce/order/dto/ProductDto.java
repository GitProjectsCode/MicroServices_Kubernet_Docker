package com.ecommerce.order.dto;

import java.math.BigDecimal;

/**
 * DTO representing product data returned from Product Microservice.
 */
public record ProductDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String category
) {}
