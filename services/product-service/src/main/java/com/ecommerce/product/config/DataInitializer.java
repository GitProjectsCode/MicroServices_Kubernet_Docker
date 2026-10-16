package com.ecommerce.product.config;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Initializes sample product catalog records upon startup if the MySQL database is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (productRepository.count() == 0) {
                log.info("Database is empty. Seeding initial e-commerce products into MySQL...");
                List<Product> initialProducts = List.of(
                        new Product("Laptop Pro 16", "High-performance developer workstation with 32GB RAM", new BigDecimal("1299.99"), "Electronics"),
                        new Product("Wireless Noise-Cancelling Headphones", "Active noise cancelling with 40-hour battery life", new BigDecimal("199.99"), "Electronics"),
                        new Product("Ergonomic Office Chair", "Adjustable lumbar support with breathable mesh", new BigDecimal("249.99"), "Furniture"),
                        new Product("Mechanical Gaming Keyboard", "Hot-swappable RGB mechanical switches", new BigDecimal("89.99"), "Electronics"),
                        new Product("Ultra-Wide 34-inch Monitor", "Curved IPS display with 144Hz refresh rate", new BigDecimal("499.99"), "Electronics")
                );
                productRepository.saveAll(initialProducts);
                log.info("Successfully seeded {} products into MySQL database.", initialProducts.size());
            } else {
                log.info("Products already present in MySQL database (count={}). Skipping seed.", productRepository.count());
            }
        } catch (Exception e) {
            log.warn("Could not seed initial products into database: {}. Application will continue.", e.getMessage());
        }
    }
}
