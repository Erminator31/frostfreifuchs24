package com.example.fff.Services;

import com.example.fff.api.ProductManager;
import com.example.fff.api.WarenausgangManager;
import com.example.fff.api.WareneingangManager;
import com.example.fff.model.Product;
import com.example.fff.model.WarenausgangItem;
import com.example.fff.model.WareneingangItem;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class to handle the generation of historical outgoing and incoming goods data
 * and coordinate operations between ProductManager, WarenausgangManager, and WareneingangManager.
 * Provides functionality for creating outgoing goods data within specific date ranges,
 * generating seasonal goods data, and ensuring that certain products exist in the system.
 */
@Service
public class HistoryService {

    ProductManager productManager;
    WarenausgangManager warenausgangManager;
    WareneingangManager wareneingangManager;
    private static final Logger LOGGER = Logger.getLogger(HistoryService.class.getName());


    /**
     * Generates and records warenausgänge (outgoing goods) for a specified date range.
     * Warenausgänge are generated per day within the range, based on seasonal patterns, and recorded
     * with specific timestamps. If stock levels after the warenausgang fall below the reorder point
     * of a product, an automatic wareneingang (incoming goods) is created to replenish stock.
     *
     * @param startDate The beginning date of the range for which warenausgänge are to be generated.
     * @param endDate   The ending date of the range for which warenausgänge are to be generated.
     * @throws Exception If there is an issue in generating or recording warenausgänge.
     */
    public void generateWarenausgaengeForDateRange(LocalDate startDate, LocalDate endDate) throws Exception {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            int m = current.getMonthValue();
            double p1, p2, p3;
            if (m == 12 || m == 1) {
                p1 = 0.6; p2 = 0.05; p3 = 0.35;
            } else if (m >= 2 && m <= 5) {
                p1 = 0.35; p2 = 0.55; p3 = 0.1;
            } else if (m >= 6 && m <= 8) {
                p1 = 0.1; p2 = 0.85; p3 = 0.05;
            } else {
                p1 = 0.35; p2 = 0.55; p3 = 0.1;
            }
            int warenausgaengeHeute = ThreadLocalRandom.current().nextInt(1, 6);
            for (int i = 0; i < warenausgaengeHeute; i++) {
                int itemCount = ThreadLocalRandom.current().nextInt(1, 4);
                Set<Integer> addedProductIds = new HashSet<>();
                List<WarenausgangItem> items = new ArrayList<>();
                for (int j = 0; j < itemCount; j++) {
                    WarenausgangItem newItem = generateWarenausgangItemWithSeason(p1, p2, p3);
                    if (!addedProductIds.contains(newItem.getProductId())) {
                        items.add(newItem);
                        addedProductIds.add(newItem.getProductId());
                    } else {
                        LOGGER.log(Level.WARNING, "Duplicate productId " + newItem.getProductId() + " skipped.");
                    }
                }
                int randomHour   = ThreadLocalRandom.current().nextInt(8, 18);
                int randomMinute = ThreadLocalRandom.current().nextInt(0, 60);
                LocalDateTime dateTime = LocalDateTime.of(current.getYear(), current.getMonthValue(), current.getDayOfMonth(), randomHour, randomMinute);
                Timestamp timestamp = Timestamp.valueOf(dateTime);
                warenausgangManager.createWarenausgang(items, timestamp);
                for (WarenausgangItem item : items) {
                    Product updatedProduct = getProductById(item.getProductId());
                    if (updatedProduct != null && updatedProduct.getProductQuantity() < updatedProduct.getReorderPoint()) {
                        WareneingangItem wareneingangItem = new WareneingangItem(updatedProduct.getProductId(), updatedProduct.getReorderQuantity());
                        wareneingangManager.createWareneingang(Collections.singletonList(wareneingangItem), timestamp, "automatic");
                    }
                }
            }
            current = current.plusDays(1);
        }
    }

    /**
     * Generates a WarenausgangItem with a product ID based on the provided probabilities
     * and a random quantity between 1 and 24. The product ID is determined randomly
     * according to the probabilities p1, p2, and p3, which should add up to 1.0 for accurate results.
     *
     * @param p1 The probability of selecting product ID 1.
     * @param p2 The probability of selecting product ID 2.
     * @param p3 The probability of selecting product ID 3.
     * @return A WarenausgangItem containing the randomly selected product ID and quantity.
     */
    public WarenausgangItem generateWarenausgangItemWithSeason(double p1, double p2, double p3) {
        double rnd = Math.random();
        int productId;
        if (rnd <= p1) {
            productId = 1;
        } else if (rnd <= p1 + p2) {
            productId = 2;
        } else {
            productId = 3;
        }
        int quantity = ThreadLocalRandom.current().nextInt(1, 25);
        if (quantity <= 0) {
            quantity = 1;
        }
        return new WarenausgangItem(productId, quantity);
    }

    /**
     * Retrieves a product by its unique identifier.
     *
     * @param productId The unique identifier of the product to be retrieved.
     * @return The {@code Product} object corresponding to the given {@code productId},
     *         or {@code null} if an error occurs or the product could not be found.
     */
    public Product getProductById(int productId) {
        try {
            return productManager.readProductById(productId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ensures that specific products exist within the system by checking for their availability
     * and adding them if they are missing. This method validates the existence of three predefined
     * products with IDs 1, 2, and 3. If any of these products do not exist in the system, they are
     * added with specific attributes.
     *
     * @throws Exception If there is an issue while reading or adding products.
     */
    public void ensureProductsExist() throws Exception {
        List<Product> existing = productManager.readProducts(null, null);
        boolean has1 = existing.stream().anyMatch(p -> p.getProductId() == 1);
        boolean has2 = existing.stream().anyMatch(p -> p.getProductId() == 2);
        boolean has3 = existing.stream().anyMatch(p -> p.getProductId() == 3);
        if (!has1) {
            productManager.addProduct("Klaus Winter", "With Antifreeze", 1000);
        }
        if (!has2) {
            productManager.addProduct("Klaus Summer", "Without Antifreeze", 1000);
        }
        if (!has3) {
            productManager.addProduct("Klaus Xtreme", "With Antifreeze", 1000);
        }
    }
}
