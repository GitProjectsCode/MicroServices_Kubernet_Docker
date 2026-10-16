package com.ecommerce.inventory.config;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes inventory stock in MySQL for catalog products if table is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final InventoryRepository inventoryRepository;

    public DataInitializer(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (inventoryRepository.count() == 0) {
                log.info("Inventory table is empty. Seeding initial stock into MySQL...");
                List<Inventory> initialStock = List.of(
                        new Inventory(1L, 100, "WH-CENTRAL-A1"),
                        new Inventory(2L, 75,  "WH-EAST-B3"),
                        new Inventory(3L, 50,  "WH-WEST-C2"),
                        new Inventory(4L, 120, "WH-CENTRAL-A4"),
                        new Inventory(5L, 40,  "WH-NORTH-D1")
                );
                inventoryRepository.saveAll(initialStock);
                log.info("Successfully seeded inventory for {} products.", initialStock.size());
            } else {
                log.info("Inventory records already present in MySQL database (count={}). Skipping seed.", inventoryRepository.count());
            }
        } catch (Exception e) {
            log.warn("Could not seed initial inventory records into database: {}. Application will continue.", e.getMessage());
        }
    }
}
