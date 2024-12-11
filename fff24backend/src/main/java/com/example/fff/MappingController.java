    package com.example.fff;

    import com.example.fff.api.OrderManager;
    import com.example.fff.api.ProductManager;
    import com.example.fff.databse.PostgresDBOrderManagement;
    import com.example.fff.databse.PostgresDBProductManagement;
    import com.example.fff.model.Order;
    import com.example.fff.model.OrderItem;
    import com.example.fff.model.Product;
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


    // Allow cross-origin requests from any source
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    // Indicates that the class is a REST controller
    @RestController
    // Base path for all endpoints in this controller
    @RequestMapping("/api")
    public class MappingController {

        ProductManager productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
        OrderManager orderManager = PostgresDBOrderManagement.getInstance();

        private static final Logger LOGGER = Logger.getLogger(MappingController.class.getName());


        /**
         * Retrieves information based on the provided name.
         *
         * @param name the name parameter for the info retrieval
         * @return a string indicating the success of the operation
         */
        @GetMapping("/auth")
        public String getInfo(@RequestParam(value = "name", defaultValue = "Name") String name) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController auth " + name);
            return "ok";
        }


        /**
         * Method to create a product table. This method logs the action, checks for token authentication, and then calls the createProductTable method in the ProductManager.
         *
         * @return a string "ok" indicating the success of the operation
         * @throws Exception if an error occurs during table creation
         */
        @GetMapping("/create-products-table")
        public String createProductTable() throws Exception {
            Logger.getLogger("MappingController")
                    .log(Level.INFO, "MappingController create-product-table ");

            // Check token

            productManager.createProductTable();

            return "ok";
        }
        @GetMapping("/create-orders-table")
        public String createOrdersTable() throws Exception {
            Logger.getLogger("MappingController")
                    .log(Level.INFO, "MappingController create-orders-table ");

            // Check token

            orderManager.createOrderTable();

            return "ok";
        }

        @GetMapping("/create-orderitems-table")
        public String createOrderItemsTable() throws Exception {
            Logger.getLogger("MappingController")
                    .log(Level.INFO, "MappingController create-orders-table ");

            // Check token

            orderManager.createOrderItemTable();

            return "ok";
        }

        /**
         * Adds a new product to the system.
         *
         * @param product the Product object to be added
         * @return ResponseEntity containing a success message upon successful addition of the product
         */
        @PostMapping(path = "/product/add", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
        public ResponseEntity<?> addProduct(@RequestBody Product product) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController POST /products/add " + product.getProductName());
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


        /**
         * Retrieves a list of products based on the provided product name and type.
         *
         * @param productName the name of the product to filter by (optional)
         * @param productType the type of the product to filter by (optional)
         * @return ResponseEntity<List < Product>> containing the list of products meeting the specified criteria
         */
        @GetMapping("/products")
        public ResponseEntity<List<Product>> getProducts(
                @RequestParam(value = "productName", required = false) String productName,
                @RequestParam(value = "productType", required = false) String productType) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController /api/inventory");

            // Produkte gefiltert aus der DB laden
            List<Product> products = productManager.readProducts(productName, productType);

            if (products.isEmpty()) {
                // Wenn keine Produkte vorhanden, 204 No Content zurückgeben
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(products);
        }

        /**
         * Removes a product from the system based on the provided product ID.
         *
         * @param productId the ID of the product to be removed
         * @return ResponseEntity containing a success message if the product is removed successfully,
         *         ResponseEntity with HTTP status 404 if the product is not found,
         *         or ResponseEntity with HTTP status 500 if an error occurs during the removal process
         */
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
        @GetMapping("/delete-orders-table")
        public ResponseEntity<String> deleteOrderTable() {
            try {
                orderManager.deleteOrderTable();
                return ResponseEntity.ok("Products table order successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting order table: " + e.getMessage());
            }
        }

        @GetMapping("/delete-orderitems-table")
        public ResponseEntity<String> deleteOrderItemsTable() {
            try {
                orderManager.deleteOrderItemsTable();
                return ResponseEntity.ok("orderitems table order successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting orderitems table: " + e.getMessage());
            }
        }


        // Neue Order-Klasse anpassen, damit optional ein orderDate Feld möglich ist.
        @PostMapping("/order")
        public ResponseEntity<?> createOrder(@RequestBody Order orderRequest,
                                             @RequestParam(value = "orderDate", required = false) String orderDateStr) {
            LOGGER.log(Level.INFO, "Creating order for customer: " + orderRequest.getCustomerName());

            try {
                Timestamp orderDate = null;
                if (orderDateStr != null && !orderDateStr.trim().isEmpty()) {
                    // Beispiel: orderDateStr im Format "2023-01-15T10:00:00"
                    orderDate = Timestamp.valueOf(orderDateStr);
                }

                Order createdOrder;
                if (orderDate == null) {
                    createdOrder = orderManager.createOrder(orderRequest.getCustomerName(), orderRequest.getItems());
                } else {
                    createdOrder = orderManager.createOrder(orderRequest.getCustomerName(), orderRequest.getItems(), orderDate);
                }

                return ResponseEntity.ok(createdOrder);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Could not create order: " + e.getMessage());
            }
        }


        /**
         * Einzelne Bestellung abrufen
         */
        @GetMapping("/order/{id}")
        public ResponseEntity<?> getOrder(@PathVariable("id") int orderId) {
            try {
                Order order = orderManager.getOrder(orderId);
                if (order == null) {
                    return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(order);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error retrieving order: " + e.getMessage());
            }
        }

        /**
         * Alle Bestellungen abrufen
         */
        @GetMapping("/orders")
        public ResponseEntity<?> getAllOrders() {
            try {
                List<Order> orders = orderManager.getAllOrders();
                if (orders.isEmpty()) {
                    return ResponseEntity.noContent().build();
                }
                return ResponseEntity.ok(orders);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error retrieving orders: " + e.getMessage());
            }
        }

        @GetMapping("/generate-history")
        public ResponseEntity<String> generateHistoricalData() {
            try {

                LOGGER.log(Level.INFO, "Creating historical data.");

                ensureProductsExist();

                LocalDate startDate = LocalDate.of(2023, 1, 1);
                LocalDate endDate = LocalDate.of(2024, 11, 30);

                int ordersPerMonth = 30;
                String[] customerNames = {"Max Mustermann", "Maria Musterfrau", "Hans Huber", "Julia Schmidt", "Peter Pan"};

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

                    // Länge des Monats bestimmen
                    int lengthOfMonth = current.lengthOfMonth();

                    for (int i = 0; i < ordersPerMonth; i++) {
                        String customer = customerNames[i % customerNames.length];

                        // 2 OrderItems pro Bestellung erstellen
                        List<OrderItem> items = new ArrayList<>();
                        items.add(generateOrderItemWithSeason(p1, p2, p3));
                        items.add(generateOrderItemWithSeason(p1, p2, p3));

                        // Zufälliges Datum im aktuellen Monat
                        int randomDay = ThreadLocalRandom.current().nextInt(1, lengthOfMonth + 1);
                        LocalDate randomDate = current.withDayOfMonth(randomDay);

                        // Zufällige Uhrzeit zwischen 8 und 17 Uhr
                        int randomHour = ThreadLocalRandom.current().nextInt(8, 18);
                        int randomMinute = ThreadLocalRandom.current().nextInt(0, 60);

                        LocalDateTime orderDateTime = LocalDateTime.of(randomDate.getYear(),
                                randomDate.getMonthValue(),
                                randomDate.getDayOfMonth(),
                                randomHour,
                                randomMinute);

                        Timestamp orderTimestamp = Timestamp.valueOf(orderDateTime);

                        // Bestellung erstellen mit spezifischem Datum
                        orderManager.createOrder(customer, items, orderTimestamp);
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

        /**
         * Generiert ein OrderItem basierend auf saisonalen Wahrscheinlichkeiten.
         *
         * @param p1 Wahrscheinlichkeit für Produkt 1
         * @param p2 Wahrscheinlichkeit für Produkt 2
         * @param p3 Wahrscheinlichkeit für Produkt 3
         * @return Das generierte OrderItem
         */
        private OrderItem generateOrderItemWithSeason(double p1, double p2, double p3) {
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
            return new OrderItem(productId, quantity);
        }

        /**
         * Stellt sicher, dass die Produkte mit IDs 1, 2 und 3 existieren. Falls nicht, werden sie angelegt.
         *
         * @throws Exception Wenn ein Fehler auftritt.
         */
        private void ensureProductsExist() throws Exception {
            // Prüfen, ob Produkt 1,2,3 existieren, sonst anlegen
            List<Product> existing = productManager.readProducts(null, null);
            boolean has1 = existing.stream().anyMatch(p -> p.getProductId() == 1);
            boolean has2 = existing.stream().anyMatch(p -> p.getProductId() == 2);
            boolean has3 = existing.stream().anyMatch(p -> p.getProductId() == 3);

            // Wenn keines der Produkte existiert, legen wir sie an.
            // Hinweis: Da productid SERIAL ist, können die IDs hochzählen.
            // Falls Sie unbedingt IDs 1,2,3 möchten, löschen Sie vorher die Tabelle.
            if (!has1) {
                productManager.addProduct("Scheibenwischmittel mit Frostschutz", "Winter", 1000);
            }

            if (!has2) {
                productManager.addProduct("Scheibenwischmittel ohne Frostschutz", "Sommer", 1000);
            }

            if (!has3) {
                productManager.addProduct("Scheibenwischmittel mit extrem Frostschutz", "Extrem-Winter", 1000);
            }
        }
    }





