package com.ecommerce.order.controller;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.filter.CorrelationIdLoggingFilter;
import com.ecommerce.order.model.InstanceResponse;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller exposing Order management endpoints, health check,
 * and instance metadata for replica pod verification in Kubernetes.
 */
@RestController
@RequestMapping("/api")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final String instanceId = UUID.randomUUID().toString();
    private final String hostname;

    public OrderController(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;

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
     * GET /api/orders - Lists all customer orders.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("Fetching all orders from database");
        return ResponseEntity.ok(orderRepository.findAll());
    }

    /**
     * GET /api/orders/{id} - Fetches an order by its ID.
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        log.info("Fetching order with id: {}", id);
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * POST /api/orders - Creates a new order by communicating with Product and Inventory services.
     */
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received request to place order: productId={}, quantity={}", request.productId(), request.quantity());
        try {
            Order created = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Order placement failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/orders/health - Health check endpoint.
     */
    @GetMapping("/orders/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "order-service",
                "hostname", hostname,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * GET /api/instance - Exposes replica pod hostname and instance ID for Kubernetes load balancing validation.
     */
    @GetMapping("/instance")
    public ResponseEntity<InstanceResponse> getInstance(HttpServletRequest request) {
        String correlationId = request.getHeader(CorrelationIdLoggingFilter.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        log.info("Instance endpoint invoked. Handled by pod: {} (instanceId: {})", hostname, instanceId);

        InstanceResponse response = new InstanceResponse(
                "order-service",
                hostname,
                instanceId,
                Instant.now(),
                correlationId
        );
        return ResponseEntity.ok(response);
    }
}
