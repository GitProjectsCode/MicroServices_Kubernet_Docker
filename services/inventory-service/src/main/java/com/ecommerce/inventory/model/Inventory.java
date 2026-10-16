package com.ecommerce.inventory.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity representing the inventory stock level of a product in warehouse.
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 100)
    private String warehouseLocation;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Inventory() {
    }

    public Inventory(Long productId, Integer quantity, String warehouseLocation) {
        this.productId = productId;
        this.quantity = quantity;
        this.warehouseLocation = warehouseLocation;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    public String getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
