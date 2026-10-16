package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.filter.CorrelationIdLoggingFilter;
import com.ecommerce.inventory.model.InstanceResponse;
import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller exposing inventory stock checks, updates, health check,
 * and pod replica metadata for Kubernetes testing.
 */
@RestController
@RequestMapping("/api")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryRepository inventoryRepository;
    private final String instanceId = UUID.randomUUID().toString();
    private final String hostname;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;

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
     * GET /api/inventory/{productId} - Retrieves real-time stock level for a product.
     */
    @GetMapping("/inventory/{productId}")
    public ResponseEntity<Inventory> getInventory(@PathVariable Long productId) {
        log.info("Checking inventory for product: {}", productId);
        return inventoryRepository.findByProductId(productId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * PUT /api/inventory/{productId}?quantity=X - Modifies stock (deducts if negative, restocks if positive).
     */
    @PutMapping("/inventory/{productId}")
    public ResponseEntity<?> updateInventory(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        log.info("Updating inventory for product: {} with quantity delta: {}", productId, quantity);

        return inventoryRepository.findByProductId(productId)
                .map(inv -> {
                    int newQty = inv.getQuantity() + quantity;
                    if (newQty < 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "Insufficient inventory. Current stock: " + inv.getQuantity()));
                    }
                    inv.setQuantity(newQty);
                    inv.setUpdatedAt(Instant.now());
                    Inventory saved = inventoryRepository.save(inv);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * GET /api/inventory/health - Health check endpoint.
     */
    @GetMapping("/inventory/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "inventory-service",
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
                "inventory-service",
                hostname,
                instanceId,
                Instant.now(),
                correlationId
        );
        return ResponseEntity.ok(response);
    }
}
