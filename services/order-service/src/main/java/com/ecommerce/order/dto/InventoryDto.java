package com.ecommerce.order.dto;

/**
 * DTO representing inventory data returned from Inventory Microservice.
 */
public record InventoryDto(
        Long productId,
        Integer quantity,
        String warehouseLocation
) {}
