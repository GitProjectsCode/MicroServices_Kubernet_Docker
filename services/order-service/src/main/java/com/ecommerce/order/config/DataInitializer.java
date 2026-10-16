package com.ecommerce.order.config;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Initializes sample order records in MySQL if the database table is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final OrderRepository orderRepository;

    public DataInitializer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (orderRepository.count() == 0) {
                log.info("Order table is empty. Seeding initial order into MySQL...");
                Order sampleOrder = new Order(
                        "ORD-INIT-001",
                        1L,
                        "Laptop Pro 16",
                        1,
                        new BigDecimal("1299.99"),
                        new BigDecimal("1299.99"),
                        "CONFIRMED"
                );
                orderRepository.save(sampleOrder);
                log.info("Successfully seeded initial order: {}", sampleOrder.getOrderNumber());
            } else {
                log.info("Orders already present in MySQL database (count={}). Skipping seed.", orderRepository.count());
            }
        } catch (Exception e) {
            log.warn("Could not seed initial orders into database: {}. Application will continue.", e.getMessage());
        }
    }
}
