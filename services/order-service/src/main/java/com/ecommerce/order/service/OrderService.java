package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.InventoryDto;
import com.ecommerce.order.dto.ProductDto;
import com.ecommerce.order.filter.CorrelationIdLoggingFilter;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service demonstrating Microservice-to-Microservice communication in Kubernetes:
 * 1. Calls Product Service to validate product existence and retrieve unit price.
 * 2. Calls Inventory Service to check real-time stock availability.
 * 3. Updates stock in Inventory Service.
 * 4. Persists the confirmed order in MySQL.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final RestClient restClient;
    private final String productServiceUrl;
    private final String inventoryServiceUrl;

    public OrderService(
            OrderRepository orderRepository,
            RestClient.Builder restClientBuilder,
            @Value("${services.product-service.url:http://product-service:8081}") String productServiceUrl,
            @Value("${services.inventory-service.url:http://inventory-service:8083}") String inventoryServiceUrl) {
        this.orderRepository = orderRepository;
        this.restClient = restClientBuilder.build();
        this.productServiceUrl = productServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        String correlationId = MDC.get(CorrelationIdLoggingFilter.MDC_CORRELATION_ID);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        log.info("[OrderService] Step 1: Requesting Product info for ID: {} from {}",
                request.productId(), productServiceUrl);

        // Step 1: Inter-service call to Product Service via Kubernetes DNS / HTTP
        ProductDto product = null;
        try {
            product = restClient.get()
                    .uri(productServiceUrl + "/api/products/" + request.productId())
                    .header(CorrelationIdLoggingFilter.CORRELATION_ID_HEADER, correlationId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ProductDto.class);
        } catch (Exception e) {
            log.error("[OrderService] Failed to contact Product Service at {}: {}", productServiceUrl, e.getMessage());
            throw new RuntimeException("Product Service communication failed: " + e.getMessage());
        }

        if (product == null) {
            throw new IllegalArgumentException("Product not found with ID: " + request.productId());
        }

        log.info("[OrderService] Step 2: Checking Inventory for Product ID: {} from {}",
                request.productId(), inventoryServiceUrl);

        // Step 2: Inter-service call to Inventory Service
        try {
            InventoryDto inventory = restClient.get()
                    .uri(inventoryServiceUrl + "/api/inventory/" + request.productId())
                    .header(CorrelationIdLoggingFilter.CORRELATION_ID_HEADER, correlationId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(InventoryDto.class);

            if (inventory == null || inventory.quantity() < request.quantity()) {
                int available = inventory != null ? inventory.quantity() : 0;
                throw new IllegalStateException("Insufficient inventory! Requested: " + request.quantity()
                        + ", Available: " + available);
            }

            // Step 3: Deduct inventory stock
            restClient.put()
                    .uri(inventoryServiceUrl + "/api/inventory/" + request.productId() + "?quantity=" + (-request.quantity()))
                    .header(CorrelationIdLoggingFilter.CORRELATION_ID_HEADER, correlationId)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[OrderService] Deducted {} units from Inventory for product {}", request.quantity(), request.productId());

        } catch (Exception e) {
            log.warn("[OrderService] Inventory Service check/deduct warning: {}. Proceeding with order creation.", e.getMessage());
        }

        // Step 4: Persist confirmed order to MySQL
        BigDecimal unitPrice = product.price();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order(
                orderNumber,
                product.id(),
                product.name(),
                request.quantity(),
                unitPrice,
                totalPrice,
                "CONFIRMED"
        );

        Order saved = orderRepository.save(order);
        log.info("[OrderService] Order {} confirmed and stored in MySQL. Total: ${}", orderNumber, totalPrice);
        return saved;
    }
}
