package com.ecommerce.product.controller;

import com.ecommerce.product.filter.CorrelationIdLoggingFilter;
import com.ecommerce.product.model.InstanceResponse;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
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
 * REST Controller exposing Product management endpoints, health check,
 * instance metadata for load balancing verification, and fault simulation.
 */
@RestController
@RequestMapping("/api")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository productRepository;
    private final String instanceId = UUID.randomUUID().toString();
    private final String hostname;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;

        // Resolve pod hostname from Kubernetes environment or local machine
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
     * GET /api/products - Lists all available products.
     */
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Fetching all products from database");
        return ResponseEntity.ok(productRepository.findAll());
    }

    /**
     * GET /api/products/{id} - Fetches a single product by ID.
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        log.info("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * POST /api/products - Creates a new product.
     */
    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        log.info("Creating new product: {}", product.getName());
        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/products/health - Health check endpoint.
     */
    @GetMapping("/products/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "product-service",
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
                "product-service",
                hostname,
                instanceId,
                Instant.now(),
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/products/slow?delayMs=3000
     * Simulates downstream latency/timeout for testing Gateway resilience and circuit breaking (Experiment 6).
     */
    @GetMapping("/products/slow")
    public ResponseEntity<Map<String, Object>> simulateSlowResponse(
            @RequestParam(defaultValue = "3000") long delayMs) {
        log.warn("Simulating slow response of {}ms on pod: {}", delayMs, hostname);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ResponseEntity.ok(Map.of(
                "service", "product-service",
                "hostname", hostname,
                "delayMs", delayMs,
                "message", "Slow response completed successfully",
                "timestamp", Instant.now().toString()
        ));
    }
}
