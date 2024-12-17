package com.example.fff;

import com.example.fff.api.ProductManager;
import com.example.fff.api.WarenausgangManager;
import com.example.fff.databse.PostgresDBProductManagement;
import com.example.fff.databse.PostgresDBWarenausgangManagement;
import com.example.fff.model.Product;
import com.example.fff.model.Warenausgang;
import com.example.fff.model.WarenausgangItem;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api")
public class MappingController {

    ProductManager productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
    WarenausgangManager warenausgangManager = PostgresDBWarenausgangManagement.getInstance();

    private static final Logger LOGGER = Logger.getLogger(MappingController.class.getName());

    @GetMapping("/auth")
    public String getInfo(@RequestParam(value = "name", defaultValue = "Name") String name) {
        Logger.getLogger("MappingController").log(Level.INFO, "MappingController auth " + name);
        return "ok";
    }

    @GetMapping("/create-products-table")
    public String createProductTable() throws Exception {
        Logger.getLogger("MappingController").log(Level.INFO, "MappingController create-product-table ");
        productManager.createProductTable();
        return "ok";
    }

    @GetMapping("/create-warenausgang-table")
    public String createWarenausgangTable() throws Exception {
        Logger.getLogger("MappingController").log(Level.INFO, "MappingController create-warenausgang-table ");
        warenausgangManager.createWarenausgangTable();
        return "ok";
    }

    @GetMapping("/create-warenausgangitem-table")
    public String createWarenausgangItemTable() throws Exception {
        Logger.getLogger("MappingController").log(Level.INFO, "MappingController create-warenausgangitem-table ");
        warenausgangManager.createWarenausgangItemTable();
        return "ok";
    }

    @PostMapping(path = "/product/add", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> addProduct(@RequestBody Product product) {
        Logger.getLogger("MappingController").log(Level.INFO, "MappingController POST /product/add " + product.getProductName());
        try {
            Product createdOrUpdated = productManager.addProduct(
                    product.getProductName(),
                    product.getProductType(),
                    product.getProductQuantity()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Product " + product.getProductName() + " added/updated successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "productType", required = false) String productType) {
        Logger.getLogger("MappingController").log(Level.INFO, "MappingController /api/products");

        List<Product> products = productManager.readProducts(productName, productType);

        if (products.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(products);
    }

    @DeleteMapping("/product/delete/{id}")
    public ResponseEntity<?> removeProduct(@PathVariable("id") int productId) {
        Logger.getLogger("MappingController").log(Level.INFO, "MappingController DELETE /product/" + productId);
        try {
            boolean removed = productManager.removeProduct(productId);
            if (removed) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Product with ID " + productId + " removed successfully.");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing product: " + e.getMessage());
        }
    }

    @GetMapping("/delete-products-table")
    public ResponseEntity<String> deleteProductsTable() {
        try {
            productManager.deleteProductsTable();
            return ResponseEntity.ok("Products table deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting products table: " + e.getMessage());
        }
    }

    @GetMapping("/delete-warenausgang-table")
    public ResponseEntity<String> deleteWarenausgangTable() {
        try {
            warenausgangManager.deleteWarenausgangTable();
            return ResponseEntity.ok("Warenausgang table deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting warenausgang table: " + e.getMessage());
        }
    }

    @GetMapping("/delete-warenausgangitem-table")
    public ResponseEntity<String> deleteWarenausgangItemsTable() {
        try {
            warenausgangManager.deleteWarenausgangItemsTable();
            return ResponseEntity.ok("Warenausgang items table deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting warenausgangitems table: " + e.getMessage());
        }
    }

    @PostMapping("/warenausgang")
    public ResponseEntity<?> createWarenausgang(@RequestBody Warenausgang warenausgangRequest,
                                                @RequestParam(value = "warenausgangDate", required = false) String warenausgangDateStr) {
        LOGGER.log(Level.INFO, "Creating warenausgang.");

        try {
            Timestamp warenausgangDate = null;
            if (warenausgangDateStr != null && !warenausgangDateStr.trim().isEmpty()) {
                warenausgangDate = Timestamp.valueOf(warenausgangDateStr);
            }

            Warenausgang createdWarenausgang;
            if (warenausgangDate == null) {
                createdWarenausgang = warenausgangManager.createWarenausgang(warenausgangRequest.getItems());
            } else {
                createdWarenausgang = warenausgangManager.createWarenausgang(warenausgangRequest.getItems(), warenausgangDate);
            }

            return ResponseEntity.ok(createdWarenausgang);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Could not create warenausgang: " + e.getMessage());
        }
    }

    @GetMapping("/warenausgang/{id}")
    public ResponseEntity<?> getWarenausgang(@PathVariable("id") int warenausgangId) {
        try {
            Warenausgang warenausgang = warenausgangManager.getWarenausgang(warenausgangId);
            if (warenausgang == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(warenausgang);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving warenausgang: " + e.getMessage());
        }
    }

    @GetMapping("/warenausgaenge")
    public ResponseEntity<?> getAllWarenausgaenge() {
        try {
            List<Warenausgang> warenausgaenge = warenausgangManager.getAllWarenausgaenge();
            if (warenausgaenge.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(warenausgaenge);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving warenausgaenge: " + e.getMessage());
        }
    }

    @GetMapping("/generate-history")
    public ResponseEntity<String> generateHistoricalData() {
        try {
            LOGGER.log(Level.INFO, "Creating historical warenausgang data.");

            ensureProductsExist();

            LocalDate startDate = LocalDate.of(2010, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 11, 30);

            int ausgaengeProMonat = 90;

            LocalDate current = startDate.withDayOfMonth(1);

            while (!current.isAfter(endDate)) {
                int m = current.getMonthValue();
                double p1, p2, p3; // Wahrscheinlichkeiten für Produkt 1, 2, 3
                if (m == 12 || m == 1 || m == 2) {
                    // Winter
                    p1 = 0.4; p2 = 0.2; p3 = 0.4;
                } else if (m >= 3 && m <= 5) {
                    // Frühling
                    p1 = 0.3; p2 = 0.5; p3 = 0.2;
                } else if (m >= 6 && m <= 8) {
                    // Sommer
                    p1 = 0.1; p2 = 0.7; p3 = 0.2;
                } else {
                    // Herbst (9,10,11)
                    p1 = 0.3; p2 = 0.3; p3 = 0.4;
                }

                int lengthOfMonth = current.lengthOfMonth();

                for (int i = 0; i < ausgaengeProMonat; i++) {
                    List<WarenausgangItem> items = new ArrayList<>();
                    items.add(generateWarenausgangItemWithSeason(p1, p2, p3));
                    items.add(generateWarenausgangItemWithSeason(p1, p2, p3));

                    // Zufälliges Datum im aktuellen Monat
                    int randomDay = ThreadLocalRandom.current().nextInt(1, lengthOfMonth + 1);
                    LocalDate randomDate = current.withDayOfMonth(randomDay);

                    // Zufällige Uhrzeit zwischen 8 und 17 Uhr
                    int randomHour = ThreadLocalRandom.current().nextInt(8, 18);
                    int randomMinute = ThreadLocalRandom.current().nextInt(0, 60);

                    LocalDateTime warenausgangDateTime = LocalDateTime.of(randomDate.getYear(),
                            randomDate.getMonthValue(),
                            randomDate.getDayOfMonth(),
                            randomHour,
                            randomMinute);

                    Timestamp warenausgangTimestamp = Timestamp.valueOf(warenausgangDateTime);

                    // Warenausgang erstellen
                    warenausgangManager.createWarenausgang(items, warenausgangTimestamp);
                }

                current = current.plusMonths(1);
            }

            return ResponseEntity.ok("Historical data generated successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating historical data: " + e.getMessage());
        }
    }

    private WarenausgangItem generateWarenausgangItemWithSeason(double p1, double p2, double p3) {
        double rnd = Math.random();
        int productId;
        if (rnd <= p1) {
            productId = 1;
        } else if (rnd <= p1 + p2) {
            productId = 2;
        } else {
            productId = 3;
        }

        // Menge zwischen 5 und 20
        int quantity = 5 + ThreadLocalRandom.current().nextInt(0, 16);
        return new WarenausgangItem(productId, quantity);
    }

    private void ensureProductsExist() throws Exception {
        // Prüfen, ob Produkte 1, 2, 3 existieren, sonst anlegen
        List<Product> existing = productManager.readProducts(null, null);
        boolean has1 = existing.stream().anyMatch(p -> p.getProductId() == 1);
        boolean has2 = existing.stream().anyMatch(p -> p.getProductId() == 2);
        boolean has3 = existing.stream().anyMatch(p -> p.getProductId() == 3);

        if (!has1) {
            productManager.addProduct("Klaus Winter", "Mit Frostschutz", 1000);
        }
        if (!has2) {
            productManager.addProduct("Klaus Summer", "Mit Frostschutz", 1000);
        }
        if (!has3) {
            productManager.addProduct("Klaus Xtreme", "Mit Frostschutz", 1000);
        }
    }

    @GetMapping("/update-daily-demand")
    public ResponseEntity<String> updateDailyDemand() {
        try {
            productManager.updateDailyDemand();
            return ResponseEntity.ok("dailyDemand updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating dailyDemand: " + e.getMessage());
        }
    }

}
