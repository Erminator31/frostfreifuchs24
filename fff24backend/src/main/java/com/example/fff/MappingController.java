    package com.example.fff;

    import com.example.fff.api.ProductManager;
    import com.example.fff.api.WarenausgangManager;
    import com.example.fff.api.WareneingangManager;
    import com.example.fff.databse.PostgresDBProductManagement;
    import com.example.fff.databse.PostgresDBWarenausgangManagement;
    import com.example.fff.databse.PostgresDBWareneingangManagement;
    import com.example.fff.model.*;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.scheduling.annotation.EnableScheduling;
    import org.springframework.web.bind.annotation.*;

    import java.sql.*;
    import java.time.LocalDate;
    import java.time.DayOfWeek;
    import java.time.MonthDay;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.*;
    import java.util.concurrent.ThreadLocalRandom;
    import java.util.logging.Level;
    import java.util.logging.Logger;

    /**
     * This class represents a MappingController that handles mapping endpoints for managing products, warenausgangs, wareneingangs, and forecasts.
     * It contains methods for retrieving information, creating tables, adding and removing products, creating warenausgangs and wareneingangs,
     * generating historical data, updating daily demand, getting forecasts, and managing weather and season factors.
     * It also includes private helper methods for generating warenausgaenge, fetching weather data, and calculating factors.

     * Fields:
     * productManager - interface for managing products
     * warenausgangManager - manager for warenausgangs
     * wareneingangManager - manager for wareneingangs
     * LOGGER - Logger for logging messages

     * Methods:
     * - getInfo: Retrieves information with a given name
     * - createProductTable: Creates a table for products
     * - createWarenausgangTable: Creates a table for warenausgangs
     * - createWarenausgangItemTable: Creates a table for warenausgang items
     * - addProduct: Adds a product to the system
     * - getProducts: Retrieves a list of products based on given criteria
     * - removeProduct: Removes a product by ID
     * - deleteProductsTable: Deletes the products table
     * - deleteWarenausgangTable: Deletes the warenausgang table
     * - deleteWarenausgangItemsTable: Deletes the warenausgang items table
     * - createWarenausgang: Creates a warenausgang entry
     * - getWarenausgang: Retrieves a warenausgang by ID
     * - getAllWarenausgaenge: Retrieves all warenausgaenge
     * - generateHistoricalData: Generates historical data for a given year and month
     * - generateWarenausgaengeForDateRange: Helper method for generating warenausgaenge within a date range
     * - getProductById: Retrieves a product by ID
     * - generateWarenausgangItemWithSeason: Helper method for generating warenausgang item with season factors
     * - ensureProductsExist: Ensures that products exist in the system
     * - updateDailyDemand: Updates the daily demand for products
     * - getAllWareneingaenge: Retrieves all wareneingaenge entries
     * - createWareneingangTable: Creates a table for wareneingangs
     * - createWareneingangItemTable: Creates a table for wareneingang items
     * - deleteWareneingangTable: Deletes the wareneingang table
     * - deleteWareneingangItemsTable: Deletes the wareneingang items table
     * - createWareneingang: Creates a wareneingang entry
     * - getForecast: Retrieves a forecast for the given days and parameters
     * - fetchWeatherData: Helper method for fetching weather data from a URL
     * - getWeatherFactor: Calculates the weather factor for a product
     * - getSeasonFactor: Calculates the season factor for a product at a given date
     * - seasonFactorProduct1: Calculates the season factor for product 1
     * - seasonFactorProduct2: Calculates the season factor for product 2
     * - seasonFactorProduct3: Calculates the season factor for product 3
     * - createForecastWeightsTable: Creates a table for forecast weights
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @RestController
    @EnableScheduling
    @RequestMapping("/api")

    public class MappingController {

        /**
         * Interface for managing products in a database.
         * Provides methods for creating, reading, updating, and deleting products,
         * as well as managing forecast weights for future predictions.
         */
        ProductManager productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
        /**
         * Interface for managing warehouse outbound operations.
         * Provides methods to create, retrieve, and delete warehouse outbound data.
         */
        WarenausgangManager warenausgangManager = PostgresDBWarenausgangManagement.getInstance();
        /**
         * Interface for managing warehouse incoming goods.
         * Provides methods to create, retrieve, and delete warehouse incoming goods and their items.
         */
        WareneingangManager wareneingangManager = PostgresDBWareneingangManagement.getInstance();
        /**
         * LOGGER is a static final Logger instance used for logging within the MappingController class.
         */
        private static final Logger LOGGER = Logger.getLogger(MappingController.class.getName());

        /**
         * Retrieves information for authentication.
         *
         * @param name the name to be used for authentication
         * @return a string indicating successful authentication ("ok")
         */
        @GetMapping("/auth")
        public String getInfo(@RequestParam(value = "name", defaultValue = "Name") String name) {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController auth " + name);
            return "ok";
        }

        /**
         * Creates the product table in the database.
         * This method delegates the creation of the product table to the ProductManager instance.
         *
         * @return a string indicating the success of the operation
         * @throws Exception if an error occurs during the table creation process
         */
        @GetMapping("/create-products-table")
        public String createProductTable() throws Exception {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController create-product-table ");
            productManager.createProductTable();
            return "ok";
        }

        /**
         * Creates the Warenausgang table in the database.
         * This method delegates the creation to the WarenausgangManager.
         *
         * @return a String indicating the status of the operation ("ok" if successful)
         * @throws Exception if an error occurs during the table creation process
         */
        @GetMapping("/create-warenausgang-table")
        public String createWarenausgangTable() throws Exception {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController create-warenausgang-table ");
            warenausgangManager.createWarenausgangTable();
            return "ok";
        }

        /**
         * Creates the Warenausgang Item table.
         *
         * @throws Exception if an error occurs during the table creation process
         */
        @GetMapping("/create-warenausgangitem-table")
        public String createWarenausgangItemTable() throws Exception {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController create-warenausgangitem-table ");
            warenausgangManager.createWarenausgangItemTable();
            return "ok";
        }

        /**
         * Adds a new product with the provided details.
         *
         * @param product The product object to be added, containing name, type, and quantity.
         * @return ResponseEntity with a success message if the product is added/updated successfully, or a bad request status with an error message.
         */
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

        /**
         * Retrieves a list of products based on the provided filters.
         *
         * @param productName Optional parameter to filter products by name.
         * @param productType Optional parameter to filter products by type.
         * @return ResponseEntity with the list of products that match the given filters.
         *         Returns a 204 No Content status if no products are found.
         */
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

        /**
         * Removes a product with the specified ID from the system.
         *
         * @param productId The ID of the product to be removed
         * @return ResponseEntity representing the status of the removal operation. Returns OK with a success message if the product was removed successfully,
         *         Not Found if the product could not be found, and Internal Server Error if an error occurred during the removal process.
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

        /**
         * Deletes the products table from the database.
         *
         * @return ResponseEntity indicating the result of the operation
         */
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

        /**
         * Deletes the Warenausgang table from the database.
         *
         * @return ResponseEntity indicating the result of the deletion operation.
         */
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
        public ResponseEntity<?> getAllWarenausgaenge(
                @RequestParam(value = "from", required = false) String fromDateStr,
                @RequestParam(value = "to", required = false) String toDateStr) {
            try {
                // Falls vorhanden, parse die Datumsparameter
                Timestamp fromTimestamp = null;
                Timestamp toTimestamp = null;
                if (fromDateStr != null && !fromDateStr.isEmpty()) {
                    fromTimestamp = Timestamp.valueOf(fromDateStr + " 00:00:00");
                }
                if (toDateStr != null && !toDateStr.isEmpty()) {
                    toTimestamp = Timestamp.valueOf(toDateStr + " 23:59:59");
                }

                // Rufe die Methode im Manager mit den Zeitparametern auf
                List<Warenausgang> warenausgaenge = warenausgangManager.getWarenausgaenge(fromTimestamp, toTimestamp);

                if (warenausgaenge.isEmpty()) {
                    return ResponseEntity.noContent().build();
                }
                return ResponseEntity.ok(warenausgaenge);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error retrieving warenausgaenge: " + e.getMessage());
            }
        }




        @GetMapping("/generate-history")
        public ResponseEntity<String> generateHistoricalData(
                @RequestParam(name = "year", required = true) Integer year,
                @RequestParam(name = "month", required = true) Integer month) {
            try {
                LOGGER.log(Level.INFO, "Creating historical warenausgang data (day-by-day).");

                // Make sure we have at least 3 products available:
                ensureProductsExist();



                LocalDate startOfMonth = LocalDate.of(year, month, 1);
                LocalDate endOfMonth   = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
                if (year == 2025 && month == 2) {
                    startOfMonth = LocalDate.of(2025, 2, 1);
                    endOfMonth   = LocalDate.of(2025, 2, 2);
                }
                // Generieren der Warenausgänge (1–8 pro Tag)
                generateWarenausgaengeForDateRange(startOfMonth, endOfMonth);

                String successMsg = String.format(
                        "Historical data (day-by-day) generated successfully for %d-%02d.",
                        year, month
                );
                return ResponseEntity.ok(successMsg);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error generating historical data: " + e.getMessage());
            }
        }

        /**
         * Generiert für jeden Tag zwischen startDate und endDate
         * zwischen 1 und 8 Warenausgänge,
         * mit je 2 Items (Menge 25–30 Stück, zufällig).
         */
        private void generateWarenausgaengeForDateRange(
                LocalDate startDate,
                LocalDate endDate
        ) throws Exception {

            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                int m = current.getMonthValue();

                // Saisonale Verteilung (nur für die Produkt-Auswahl, nicht die Menge)
                double p1, p2, p3;
                if (m == 12 || m == 1) {
                    // Winter
                    p1 = 0.6; p2 = 0.05; p3 = 0.35;
                } else if (m >= 2 && m <= 5) {
                    // Frühling
                    p1 = 0.35; p2 = 0.55; p3 = 0.1;
                } else if (m >= 6 && m <= 8) {
                    // Sommer
                    p1 = 0.1; p2 = 0.85; p3 = 0.05;
                } else {
                    // Herbst (9, 10, 11)
                    p1 = 0.35; p2 = 0.55; p3 = 0.1;
                }

                // Tägliche Anzahl Warenausgänge: 1..8
                int warenausgaengeHeute = ThreadLocalRandom.current().nextInt(1, 6);

                for (int i = 0; i < warenausgaengeHeute; i++) {
                    // Hier wird entschieden, wie viele unterschiedliche Produkte in diesem Warenausgang sind: 1..3
                    int itemCount = ThreadLocalRandom.current().nextInt(1, 4);
                    Set<Integer> addedProductIds = new HashSet<>(); // Track added productIds

                    List<WarenausgangItem> items = new ArrayList<>();
                    for (int j = 0; j < itemCount; j++) {
                        WarenausgangItem newItem = generateWarenausgangItemWithSeason(p1, p2, p3);
                        if (!addedProductIds.contains(newItem.getProductId())) {
                            items.add(newItem);
                            addedProductIds.add(newItem.getProductId());
                        } else {
                            // Optionally, handle duplicates if needed (e.g., log or adjust quantity)
                            LOGGER.log(Level.WARNING, "Duplicate productId " + newItem.getProductId() + " skipped.");
                        }
                    }

                    // Zufällige Uhrzeit am aktuellen Tag (z.B. 8–17 Uhr)
                    int randomHour   = ThreadLocalRandom.current().nextInt(8, 18);
                    int randomMinute = ThreadLocalRandom.current().nextInt(0, 60);
                    LocalDateTime dateTime = LocalDateTime.of(
                            current.getYear(),
                            current.getMonthValue(),
                            current.getDayOfMonth(),
                            randomHour,
                            randomMinute
                    );
                    Timestamp timestamp = Timestamp.valueOf(dateTime);

                    // Warenausgang anlegen
                    warenausgangManager.createWarenausgang(items, timestamp);

                    // Ggf. automatischer Wareneingang, wenn Produkt < reorderPoint
                    for (WarenausgangItem item : items) {
                        Product updatedProduct = getProductById(item.getProductId());
                        if (updatedProduct != null
                                && updatedProduct.getProductQuantity() < updatedProduct.getReorderPoint()) {

                            WareneingangItem wareneingangItem = new WareneingangItem(
                                    updatedProduct.getProductId(),
                                    updatedProduct.getReorderQuantity()
                            );
                            // Wareneingang mit demselben Timestamp
                            wareneingangManager.createWareneingang(
                                    Collections.singletonList(wareneingangItem),
                                    timestamp,"automatic"
                            );
                        }
                    }
                }

                // Nächster Tag
                current = current.plusDays(1);
            }
        }


        /**
         * Wählt anhand saisonaler Wahrscheinlichkeiten aus, welches Produkt (ID 1, 2 oder 3)
         * verbraucht wird und erzeugt ein `WarenausgangItem` mit einer zufälligen
         * Menge zwischen 25 und 30 Stück.
         */
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

            int quantity = ThreadLocalRandom.current().nextInt(1, 25);

            if(quantity<=0){
                quantity=1;
            }

            return new WarenausgangItem(productId, quantity);
        }

        private Product getProductById(int productId) {
            try {
                return productManager.readProductById(productId);
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }



        private void ensureProductsExist() throws Exception {
            // Prüfen, ob Produkte 1, 2, 3 existieren, sonst anlegen
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

        @GetMapping("/wareneingaenge")
        public ResponseEntity<?> getAllWareneingaenge() {
            try {
                WareneingangManager wareneingangManager = PostgresDBWareneingangManagement.getInstance();
                List<Wareneingang> eingange = wareneingangManager.getAllWareneingaenge();
                if (eingange.isEmpty()) {
                    return ResponseEntity.noContent().build();
                }
                return ResponseEntity.ok(eingange);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error retrieving wareneingaenge: " + e.getMessage());
            }
        }

        @GetMapping("/create-wareneingang-table")
        public ResponseEntity<String> createWareneingangTable() {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController create-wareneingang-table ");
            try {
                wareneingangManager.createWareneingangTable();
                return ResponseEntity.ok("Wareneingang table created successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error creating wareneingang table: " + e.getMessage());
            }
        }

        @GetMapping("/create-wareneingangitem-table")
        public ResponseEntity<String> createWareneingangItemTable() {
            Logger.getLogger("MappingController").log(Level.INFO, "MappingController create-wareneingangitem-table ");
            try {
                wareneingangManager.createWareneingangItemTable();
                return ResponseEntity.ok("Wareneingang items table created successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error creating wareneingangitems table: " + e.getMessage());
            }
        }

        @GetMapping("/delete-wareneingang-table")
        public ResponseEntity<String> deleteWareneingangTable() {
            try {
                wareneingangManager.deleteWareneingangTable();
                return ResponseEntity.ok("Wareneingang table deleted successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting wareneingang table: " + e.getMessage());
            }
        }

        @GetMapping("/delete-wareneingangitem-table")
        public ResponseEntity<String> deleteWareneingangItemsTable() {
            try {
                wareneingangManager.deleteWareneingangItemsTable();
                return ResponseEntity.ok("Wareneingang items table deleted successfully.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting wareneingangitems table: " + e.getMessage());
            }
        }

        @PostMapping("/wareneingang")
        public ResponseEntity<?> createWareneingang(
                @RequestBody Wareneingang wareneingangRequest,
                @RequestParam(value = "wareneingangDate", required = false) String wareneingangDateStr) {

            LOGGER.log(Level.INFO, "Creating Wareneingang (manual).");
            try {
                Timestamp wareneingangDate = null;
                if (wareneingangDateStr != null && !wareneingangDateStr.trim().isEmpty()) {
                    wareneingangDate = Timestamp.valueOf(wareneingangDateStr);
                }

                Wareneingang created;
                // This call will ALWAYS be a "manual" Wareneingang
                if (wareneingangDate == null) {
                    created = wareneingangManager.createWareneingang(wareneingangRequest.getItems());
                } else {
                    created = wareneingangManager.createWareneingang(wareneingangRequest.getItems(), wareneingangDate);
                }

                return ResponseEntity.ok(created);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Could not create wareneingang: " + e.getMessage());
            }
        }


        @GetMapping("/forecast")
        public ResponseEntity<?> getForecast(
                @RequestParam(value = "forecastDays", defaultValue = "14") int forecastDays,
                @RequestParam(value = "alpha", required = false) Double alphaParam,
                @RequestParam(value = "beta", required = false) Double betaParam,
                @RequestParam(value = "gamma", required = false) Double gammaParam
        ) {
            LOGGER.log(Level.INFO, "Received /forecast request mit forecastDays=" + forecastDays);

            // Datenbank-Connection für Abfragen
            try (Connection conn = PostgresDBWarenausgangManagement.getInstance()
                    .getDataSource().getConnection()) {

                // 1) Forecast-Weights laden und ggf. updaten
                ForecastWeights currentWeights = productManager.getForecastWeights();
                double alpha = currentWeights.getAlpha();
                double beta  = currentWeights.getBeta();
                double gamma = currentWeights.getGamma();

                boolean changed = false;
                if (alphaParam != null && !alphaParam.equals(alpha)) { alpha = alphaParam; changed = true; }
                if (betaParam  != null && !betaParam.equals(beta))   { beta  = betaParam;  changed = true; }
                if (gammaParam != null && !gammaParam.equals(gamma)) { gamma = gammaParam; changed = true; }
                if (changed) {
                    productManager.updateForecastWeights(alpha, beta, gamma);
                    LOGGER.log(Level.INFO, "Forecast weights updated in DB: alpha=" + alpha
                            + ", beta=" + beta + ", gamma=" + gamma);
                }

                // 2) Wetter-API abrufen, z.B. via fetchWeatherData()
                String apiUrl = "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=49.3536"
                        + "&longitude=9.1511"
                        + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                        + "&forecast_days=" + forecastDays;
                String weatherJson = fetchWeatherData(apiUrl);
                org.json.JSONObject json = new org.json.JSONObject(weatherJson);
                org.json.JSONObject daily = json.getJSONObject("daily");
                org.json.JSONArray timeArray     = daily.getJSONArray("time");
                org.json.JSONArray tempMaxArray  = daily.getJSONArray("temperature_2m_max");
                org.json.JSONArray tempMinArray  = daily.getJSONArray("temperature_2m_min");
                org.json.JSONArray precProbArray = daily.getJSONArray("precipitation_probability_max");

                // 3) Alle Produkte laden
                List<Product> allProducts = productManager.readProducts(null, null);

                // 4) Datenstrukturen vorbereiten
                List<String> dateList = new ArrayList<>();
                DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd. MM. yyyy");

                // Map -> Produktname => Liste Forecastwerte
                Map<String, List<Double>> dataMap = new LinkedHashMap<>();
                for (Product p : allProducts) {
                    dataMap.put(p.getProductName(), new ArrayList<>());
                }

                // 5) Pro Tag Forecast berechnen
                for (int i = 0; i < timeArray.length(); i++) {
                    String dateString = timeArray.getString(i);
                    double tmax = tempMaxArray.optDouble(i, 0.0);
                    double tmin = tempMinArray.optDouble(i, 0.0);
                    double avgTemp = (tmax + tmin) / 2.0;
                    double precipProb = precProbArray.optDouble(i, 0.0);

                    LocalDate parsedDate = LocalDate.parse(dateString, inputFmt);
                    String formattedDate = parsedDate.format(outputFmt);
                    if (dateList.size() < timeArray.length()) {
                        dateList.add(formattedDate);
                    }

                    // z.B. top-2 Produkte mit highest weatherFactor => +10% bei Regen>50 usw.
                    List<ProductWFactor> wFactors = new ArrayList<>();
                    for (Product product : allProducts) {
                        double wf = getWeatherFactor(product.getProductId(), avgTemp);
                        wFactors.add(new ProductWFactor(product, wf));
                    }
                    wFactors.sort((a,b) -> Double.compare(b.weatherFactor(), a.weatherFactor()));
                    Set<Integer> top2ProductIds = new HashSet<>();
                    if (precipProb > 50.0 && wFactors.size() >= 2) {
                        top2ProductIds.add(wFactors.get(0).product().getProductId());
                        top2ProductIds.add(wFactors.get(1).product().getProductId());
                    }

                    // Forecast-Berechnung pro Produkt
                    for (Product product : allProducts) {
                        // Historischer Tagesdurchschnitt ...
                        double historicalAvg = warenausgangManager.calculateSameDayHistoricalAverage(
                                product.getProductId(), parsedDate, 5, conn
                        );
                        if (historicalAvg <= 0) {
                            historicalAvg = 5.0;
                        }

                        double weatherFactor = getWeatherFactor(product.getProductId(), avgTemp);
                        double seasonFactor  = getSeasonFactor(product.getProductId(), dateString);

                        double base = (alpha * historicalAvg)
                                * ((beta * weatherFactor + (gamma * (1.0 + seasonFactor))) / 2.0);

                        // Falls in top2 & Regen>50 => z.B. +10%
                        if (top2ProductIds.contains(product.getProductId())) {
                            base *= 1.1;
                        }
                        // Feiertags/Wochenend-Reduktion
                        if (isGermanHolidayOrWeekend(parsedDate)) {
                            double reduction = 0.10 + (Math.random() * 0.05);
                            base *= (1.0 - reduction);
                        }
                        double forecastForDay = base;
                        dataMap.get(product.getProductName()).add(forecastForDay);
                    }
                }

                // 6) EOQ für jedes Produkt berechnen und DB updaten
                // 6a) costPerItem holen
                double costPerItem = getCostPerItem();
                // 6b) Bestellkosten S = 25€
                double orderCost = 25.0;

                // Zeitfenster (Vorjahr) für Jahresbedarf
                LocalDate lastYearStart = LocalDate.now().minusYears(1).withDayOfYear(1);
                LocalDate lastYearEnd   = LocalDate.now().minusYears(1)
                        .withDayOfYear(lastYearStart.lengthOfYear());

                for (Product product : allProducts) {
                    List<Double> fcValues = dataMap.get(product.getProductName());
                    if (fcValues == null || fcValues.isEmpty()) {
                        continue;
                    }
                    // Nächsten 7 Tage für dailyDemand
                    double sum7 = 0.0;
                    int limit7 = Math.min(7, fcValues.size());
                    for (int i=0; i<limit7; i++) {
                        sum7 += fcValues.get(i);
                    }
                    int newDailyDemand = (int) Math.round(sum7 / limit7);

                    // Nächsten 3 Tage -> reorderPoint (Beispiel)
                    double sum3 = 0.0;
                    int limit3 = Math.min(3, fcValues.size());
                    for (int i=0; i<limit3; i++) {
                        sum3 += fcValues.get(i);
                    }
                    int newReorderPoint = (int) Math.round(sum3);

                    // Jahresbedarf D
                    double annualDemand = warenausgangManager.calculateDemandInPeriod(
                            product.getProductId(),
                            lastYearStart, lastYearEnd,
                            conn
                    );
                    if (annualDemand < 1) {
                        annualDemand = 50.0; // Fallback
                    }

                    // EOQ = sqrt( (2 * S * D) / H )
                    LOGGER.log(Level.INFO, "ordercost: " + orderCost + " annualdemand: " + annualDemand
                            + " costPerItem " + costPerItem + " productname: " + product.getProductName());
                    double eoq = Math.sqrt( (2.0 * orderCost * annualDemand) / costPerItem );
                    int newReorderQuantity = (int)Math.round(eoq);

                    // in DB updaten
                    productManager.updateProductForecastValues(
                            product,
                            newDailyDemand,
                            newReorderPoint,
                            newReorderQuantity
                    );
                }

                // 7) Ergebnis für das Frontend
                Map<String, Object> result = new HashMap<>();
                result.put("alphaUsed", alpha);
                result.put("betaUsed",  beta);
                result.put("gammaUsed", gamma);
                result.put("dates", dateList);
                result.put("data", dataMap);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler bei /forecast: " + e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler bei /forecast: " + e.getMessage());
            }
        }


        // Hilfsrecord für Product + weatherFactor
        private record ProductWFactor(Product product, double weatherFactor) {}



        private String fetchWeatherData(String url) throws Exception {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(
                    request,
                    java.net.http.HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw new RuntimeException("Fehler beim Abruf der Wetter-API. Status=" + response.statusCode());
            }
        }


        private record DailyTemperature(String dateString, double avgTemp) {}

        private double getWeatherFactor(int productId, double temperature) {
            if (temperature < -5) {
                return switch (productId) {
                    case 1 -> 0.7;
                    case 2 -> 0.2;
                    case 3 -> 1.0;
                    default -> 1.0; // Fallback
                };
            }
            else if (temperature <= 4) {
                return switch (productId) {
                    case 1 -> 1.0;
                    case 2 -> 0.2;
                    case 3 -> 0.5;
                    default -> 1.0;
                };
            }
            // Falls > 4 Grad
            else {
                return switch (productId) {
                    case 1 -> 0.1;
                    case 2 -> 1.0;
                    case 3 -> 0.05;
                    default -> 1.0;
                };
            }
        }

        private double getSeasonFactor(int productId, String isoDate) {
            // isoDate z.B. "2025-01-12"
            // Wir parse das Jahr,Monat,Tag -> extrahieren Monat
            java.time.LocalDate date = java.time.LocalDate.parse(isoDate);
            int monthIndex = date.getMonthValue() - 1; // 0..11

            return switch (productId) {
                case 1 -> seasonFactorProduct1(monthIndex);
                case 2 -> seasonFactorProduct2(monthIndex);
                case 3 -> seasonFactorProduct3(monthIndex);
                default -> 0.0; // Fallback
            };
        }

        private double seasonFactorProduct1(int x) {
            // g(x) = | - ( cos(π/6 * x) + 1.2 ) * 0.4 |
            double val = -(Math.cos(Math.PI / 6.0 * x) + 1.2) * 0.45;
            return Math.abs(val);
        }

        private double seasonFactorProduct2(int x) {
            // f(x) = | - ( cos(π/6 * x) - 1.2 ) * 0.4 |
            double val = -(Math.cos(Math.PI / 6.0 * x) - 1.2) * 0.45;
            return Math.abs(val);
        }

        private double seasonFactorProduct3(int x) {
            // Produkt 3:
            //   - Bei x=0 oder x=11 => wie Produkt1(x=0 bzw. x=11)
            //   - Sonst wie Produkt1(x=6)
            if (x == 0 || x == 11) {
                return seasonFactorProduct1(x);
            } else {
                // tu so, als wär x=6 => seasonFactorProduct1(6)
                return seasonFactorProduct1(6);
            }
        }


        private static final Set<MonthDay> GERMAN_HOLIDAYS = Set.of(
                MonthDay.of(1, 1),   // Neujahr
                MonthDay.of(1, 6),   // Heilige Drei Könige
                MonthDay.of(5, 1),   // Tag der Arbeit
                MonthDay.of(8, 15),  // Mariä Himmelfahrt
                MonthDay.of(10, 3),  // Tag der Deutschen Einheit
                MonthDay.of(10, 31), // Reformationstag
                MonthDay.of(11, 1),  // Allerheiligen
                MonthDay.of(12, 25), // 1. Weihnachtstag
                MonthDay.of(12, 26)  // 2. Weihnachtstag
        );


        private boolean isGermanHolidayOrWeekend(LocalDate date) {
            // Prüfen, ob das Datum in der Feiertagsliste steht:
            if (GERMAN_HOLIDAYS.contains(MonthDay.from(date))) {
                return true;
            }
            // Prüfen, ob Samstag (6) oder Sonntag (7)
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                return true;
            }
            return false;
        }



        @GetMapping("/create-forecast-weights-table")
        public ResponseEntity<String> createForecastWeightsTable() {
            LOGGER.log(Level.INFO, "MappingController create-forecast-weights-table invoked");
            try {
                // Hier rufst du die Methode in deinem DB-Manager auf,
                // die das CREATE TABLE durchführt.
                productManager.createForecastWeightsTable();

                // Gibt eine einfache OK-Nachricht zurück
                return ResponseEntity.ok("forecast_weights table created (or already exists).");
            } catch (Exception e) {
                // Fehlerbehandlung
                LOGGER.log(Level.SEVERE, "Fehler beim Erstellen der forecast_weights-Tabelle: " + e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error creating forecast_weights table: " + e.getMessage());
            }
        }


        @GetMapping("/warenausgaenge/pro-tag")
        public ResponseEntity<?> getWarenausgaengeProTag(
                @RequestParam(value = "from", required = false) String fromDateStr,
                @RequestParam(value = "to", required = false) String toDateStr) {
            try {
                // Konvertierung der Datumsstrings in Timestamps
                Timestamp fromTimestamp = null;
                Timestamp toTimestamp = null;
                DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd. MM. yyyy");

                // Setze Standardzeitraum auf letzte 14 Tage, falls keine Parameter angegeben sind
                LocalDate today = LocalDate.now();
                LocalDate defaultFrom = today.minusDays(13); // insgesamt 14 Tage inklusive heute

                if (fromDateStr != null && !fromDateStr.isEmpty()) {
                    fromTimestamp = Timestamp.valueOf(fromDateStr + " 00:00:00");
                } else {
                    fromTimestamp = Timestamp.valueOf(defaultFrom.atStartOfDay());
                }
                if (toDateStr != null && !toDateStr.isEmpty()) {
                    toTimestamp = Timestamp.valueOf(toDateStr + " 23:59:59");
                } else {
                    toTimestamp = Timestamp.valueOf(today.atTime(23, 59, 59));
                }

                // Abruf der Statistikdaten aus dem Manager
                List<TagesStatistik> statistik = warenausgangManager.getWarenausgaengeProTag(fromTimestamp, toTimestamp);

                // Alle Produkte laden
                List<Product> alleProdukte = productManager.readProducts(null, null);

                // Erstellen einer Liste aller Tage im Zeitraum
                LocalDate startDate = fromTimestamp.toLocalDateTime().toLocalDate();
                LocalDate endDate = toTimestamp.toLocalDateTime().toLocalDate();
                List<LocalDate> tageImZeitraum = new ArrayList<>();
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    tageImZeitraum.add(d);
                }

                // Initialisiere Datenstruktur: Für jedes Produkt, für jeden Tag 0
                Map<String, List<Integer>> produktDaten = new HashMap<>();
                for (Product produkt : alleProdukte) {
                    List<Integer> zahlenListe = new ArrayList<>();
                    for (int i = 0; i < tageImZeitraum.size(); i++) {
                        zahlenListe.add(0);
                    }
                    produktDaten.put(produkt.getProductName(), zahlenListe);
                }

                // Erstelle Liste der formatierten Datumsstrings für die Antwort
                List<String> dates = new ArrayList<>();
                for (LocalDate tag : tageImZeitraum) {
                    dates.add(tag.format(outputFmt));
                }

                // Fülle die Daten aus den Statistik-Ergebnissen
                for (TagesStatistik ts : statistik) {
                    // Datum parsen (im Format "yyyy-MM-dd")
                    LocalDate datum = LocalDate.parse(ts.getDatum(), inputFmt);
                    // Index des Tages in der Liste der Tage finden
                    int index = tageImZeitraum.indexOf(datum);
                    if (index != -1) {
                        String produktName = ts.getProduktName();
                        List<Integer> zahlenListe = produktDaten.get(produktName);
                        if (zahlenListe != null) {
                            // Setze die Anzahl für den entsprechenden Tag
                            zahlenListe.set(index, ts.getAnzahl());
                        }
                    }
                }

                // Baue die finale Antwortstruktur
                Map<String, Object> response = new HashMap<>();
                response.put("dates", dates);
                response.put("data", produktDaten);

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler beim Abrufen der Tagesstatistik: " + e.getMessage());
            }
        }

        @GetMapping("/wareneingaenge/pro-tag")
        public ResponseEntity<?> getWareneingaengeProTag(
                @RequestParam(value = "from", required = false) String fromDateStr,
                @RequestParam(value = "to", required = false) String toDateStr) {
            try {
                // Konvertierung der Datumsstrings in Timestamps
                Timestamp fromTimestamp = null;
                Timestamp toTimestamp = null;
                DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd. MM. yyyy");

                // Optional: Standardzeitraum festlegen (z.B. letzte 14 Tage) wenn keine Parameter angegeben sind
                LocalDate today = LocalDate.now();
                LocalDate defaultFrom = today.minusDays(13);

                if (fromDateStr != null && !fromDateStr.isEmpty()) {
                    fromTimestamp = Timestamp.valueOf(fromDateStr + " 00:00:00");
                } else {
                    fromTimestamp = Timestamp.valueOf(defaultFrom.atStartOfDay());
                }
                if (toDateStr != null && !toDateStr.isEmpty()) {
                    toTimestamp = Timestamp.valueOf(toDateStr + " 23:59:59");
                } else {
                    toTimestamp = Timestamp.valueOf(today.atTime(23, 59, 59));
                }

                // Abruf der Statistikdaten aus dem Manager
                List<TagesStatistik> statistik = wareneingangManager.getWareneingaengeProTag(fromTimestamp, toTimestamp);

                if (statistik.isEmpty()) {
                    return ResponseEntity.noContent().build();
                }

                // Weiterverarbeitung analog zu WarenausgaengeProTag, z.B. Aufbereitung pro Produkt und Tag...
                // Hier können Sie den Code aus der vorherigen getWarenausgaengeProTag-Methode anpassen.

                // Beispiel: Trennung in dates und data, gruppiert nach Produkt (analog wie zuvor)
                List<Product> alleProdukte = productManager.readProducts(null, null);

                LocalDate startDate = fromTimestamp.toLocalDateTime().toLocalDate();
                LocalDate endDate = toTimestamp.toLocalDateTime().toLocalDate();
                List<LocalDate> tageImZeitraum = new ArrayList<>();
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    tageImZeitraum.add(d);
                }

                Map<String, List<Integer>> produktDaten = new HashMap<>();
                for (Product produkt : alleProdukte) {
                    List<Integer> zahlenListe = new ArrayList<>();
                    for (int i = 0; i < tageImZeitraum.size(); i++) {
                        zahlenListe.add(0);
                    }
                    produktDaten.put(produkt.getProductName(), zahlenListe);
                }

                List<String> dates = new ArrayList<>();
                for (LocalDate tag : tageImZeitraum) {
                    dates.add(tag.format(outputFmt));
                }

                for (TagesStatistik ts : statistik) {
                    LocalDate datum = LocalDate.parse(ts.getDatum(), inputFmt);
                    int index = tageImZeitraum.indexOf(datum);
                    if (index != -1) {
                        String produktName = ts.getProduktName();
                        List<Integer> zahlenListe = produktDaten.get(produktName);
                        if (zahlenListe != null) {
                            zahlenListe.set(index, ts.getAnzahl());
                        }
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("dates", dates);
                response.put("data", produktDaten);

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler beim Abrufen der Tagesstatistik für Wareneingänge: " + e.getMessage());
            }
        }

        @PatchMapping("/products/update/{id}")
        public ResponseEntity<?> patchProduct(
                @PathVariable("id") int productId,
                @RequestBody Map<String, Object> updates) {
            Logger.getLogger("MappingController").log(Level.INFO, "Patching product with ID " + productId);
            try {
                // Prüfen, ob das Produkt existiert
                Product existing = productManager.readProductById(productId);
                if (existing == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
                }

                // Führe das partielle Update durch
                productManager.updateProductPartial(productId, updates);

                Map<String, String> response = new HashMap<>();
                response.put("message", "Product updated successfully.");
                return ResponseEntity.ok(response);

            } catch (SQLException e) {
                Logger.getLogger("MappingController").log(Level.SEVERE, "Error patching product: " + e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error updating product: " + e.getMessage());
            }
        }

        /**
         * Deletes a Warenausgang entry based on the provided ID.
         *
         * @param warenausgangId The ID of the Warenausgang entry to be deleted.
         * @return ResponseEntity<?> ResponseEntity containing the deletion status:
         *                  - If the Warenausgang was deleted successfully, returns a success message.
         *                  - If the Warenausgang was not found, returns a NOT_FOUND status with an error message.
         *                  - If an error occurs during deletion, returns an INTERNAL_SERVER_ERROR status with an error message.
         */
        @DeleteMapping("/warenausgang/delete/{id}")
        public ResponseEntity<?> deleteWarenausgang(@PathVariable("id") int warenausgangId) {
            Logger.getLogger("MappingController").log(Level.INFO, "Deleting warenausgang with ID " + warenausgangId);
            try {
                boolean deleted = warenausgangManager.deleteWarenausgang(warenausgangId);
                if (deleted) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Warenausgang with ID " + warenausgangId + " deleted successfully.");
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Warenausgang not found");
                }
            } catch (Exception e) {
                Logger.getLogger("MappingController").log(Level.SEVERE, "Error deleting warenausgang: " + e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting warenausgang: " + e.getMessage());
            }
        }

        /**
         * Retrieves the warehouse costs based on the provided parameters and current environmental data.
         *
         * @param pBasis The base price value (default: 1000).
         * @param kP The multiplier for temperature-dependent costs (default: 7000).
         * @param grundkosten The base costs for operations (default: 1000).
         * @return ResponseEntity containing JSON data with the following fields:
         *         - temperature: The current average temperature in Celsius.
         *         - electricityPriceEURperMWh: The average electricity price in EUR per MWh.
         *         - electricityPriceEURperkWh: The average electricity price in EUR per kWh.
         *         - totalQuantity: Total quantity in the warehouse.
         *         - stromverbrauchKWh: Estimated power consumption in kWh.
         *         - grundkosten: Provided base costs for operations.
         *         - stromkosten: Total electricity costs.
         *         - betriebskosten: Total operational costs including electricity and base costs.
         *         - costPerItem: Optional cost per item based on totalQuantity, calculated only if totalQuantity > 0.
         */
        @GetMapping("/lagerkosten")
        public ResponseEntity<?> getLagerkosten(
                @RequestParam(value = "pBasis", defaultValue = "1000") double pBasis,
                @RequestParam(value = "kP", defaultValue = "7000") double kP,
                @RequestParam(value = "grundkosten", defaultValue = "1000") double grundkosten
        ) {
            // 1) Fetch current temperature (for "today") via Open-Meteo
            //    We can re-use fetchWeatherData(...) but set forecast_days=1
            //    Then parse day-0 in the daily block to get average temperature
            try {
                // In a real application, adjust lat/long, and pass the local timezone if needed
                String weatherUrl = "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=49.3536"
                        + "&longitude=9.1511"
                        + "&daily=temperature_2m_max,temperature_2m_min"
                        + "&forecast_days=1"
                        + "&timezone=Europe/Berlin";

                String weatherJson = fetchWeatherData(weatherUrl);
                org.json.JSONObject json = new org.json.JSONObject(weatherJson);
                org.json.JSONObject daily = json.getJSONObject("daily");

                // We expect arrays of length=1 for "time", "temperature_2m_max", "temperature_2m_min"
                double tmax = daily.getJSONArray("temperature_2m_max").optDouble(0, 0.0);
                double tmin = daily.getJSONArray("temperature_2m_min").optDouble(0, 0.0);
                // Simple average for the day
                double currentTemperature = (tmax + tmin) / 2.0;

                // 2) Fetch electricity price for 24h from now, parse average => EUR/MWh => convert to EUR/kWh
                //    For demonstration, let's assume "now" is a Unix epoch.
                //    You can adapt “start” and “end” times to be exactly 24 hours.
                long nowUnix = System.currentTimeMillis() / 1000L;              // current time in seconds
                long oneDayLater = nowUnix + 24L * 3600L;                       // +24h in seconds

                // Example URL:
                // https://api.energy-charts.info/price?bzn=DE-LU&start=1737743000&end=1737756000
                // We'll pass nowUnix as start, oneDayLater as end
                String priceUrl = String.format(
                        "https://api.energy-charts.info/price?bzn=DE-LU&start=%d&end=%d",
                        nowUnix, oneDayLater
                );
                String priceJson = fetchElectricityPriceData(priceUrl);
                org.json.JSONObject priceObj = new org.json.JSONObject(priceJson);

                // The array "price" is in EUR / MWh, e.g. [84.99, 80.79, 91.97, 86.29]
                org.json.JSONArray priceArray = priceObj.getJSONArray("price");
                double sumPrice = 0.0;
                for (int i = 0; i < priceArray.length(); i++) {
                    sumPrice += priceArray.getDouble(i);
                }
                double avgPriceMWh = (priceArray.length() == 0) ? 0 : (sumPrice / priceArray.length());
                // Convert EUR/MWh to EUR/kWh => /1000
                double avgPriceKWh = avgPriceMWh / 1000.0;

                // 3) Sum up the current total warehouse quantity
                int totalQuantity = getTotalWarehouseQuantity();

                // 4) Apply your formula(s):
                //    If T > 0 => P = pBasis + kP*(1/T)
                //    If T < 0 => P = pBasis + kP*|T|
                double P;
                if (currentTemperature > 0) {
                    P = pBasis + kP * (1.0 / currentTemperature);
                } else {
                    // T <= 0
                    double absT = Math.abs(currentTemperature);
                    P = pBasis + kP * absT;
                }

                // Stromkosten = Stromverbrauch * Strompreis
                double stromkosten = P * avgPriceKWh;
                // Betriebskosten = Stromkosten + Grundkosten
                double betriebskosten = stromkosten + grundkosten;

                // Optional: cost per item (if totalQuantity>0)
                double costPerItem = 0.0;
                if (totalQuantity > 0) {
                    costPerItem = betriebskosten / totalQuantity;
                }

                // 5) Build JSON response
                java.util.Map<String,Object> result = new java.util.HashMap<>();
                result.put("temperature", currentTemperature);
                result.put("electricityPriceEURperMWh", avgPriceMWh);
                result.put("electricityPriceEURperkWh", avgPriceKWh);
                result.put("totalQuantity", totalQuantity);
                result.put("stromverbrauchKWh", P);
                result.put("grundkosten", grundkosten);
                result.put("stromkosten", stromkosten);
                result.put("betriebskosten", betriebskosten);
                result.put("costPerItem", costPerItem);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler in /lagerkosten: " + e.getMessage(), e);
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler bei Lagerkosten-Berechnung: " + e.getMessage());
            }
        }

        private String fetchElectricityPriceData(String url) throws Exception {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(
                    request,
                    java.net.http.HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw new RuntimeException("Fehler beim Abruf der Strompreis-API. Status=" + response.statusCode());
            }
        }
        private int getTotalWarehouseQuantity() throws SQLException {
            try (Connection conn = productManager.getDataSource().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(quantity), 0) AS total FROM products")) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
            return 0;
        }


        /**
         * Berechnet die Lagerkosten pro Stück (costPerItem), basierend auf:
         *  - aktueller Temperatur (Open-Meteo)
         *  - Strompreis (energy-charts)
         *  - Gesamtmenge im Lager (SUM(products.quantity))
         *  - pBasis, kP, grundkosten (hier beispielhaft festgelegt)
         */
        private double getCostPerItem() throws Exception {
            // 1) Temperatur aus Open-Meteo holen
            String weatherUrl = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=49.3536"
                    + "&longitude=9.1511"
                    + "&daily=temperature_2m_max,temperature_2m_min"
                    + "&forecast_days=1"
                    + "&timezone=Europe/Berlin";

            String weatherJson = fetchWeatherData(weatherUrl);
            org.json.JSONObject weatherObj = new org.json.JSONObject(weatherJson);
            org.json.JSONObject daily = weatherObj.getJSONObject("daily");
            double tmax = daily.getJSONArray("temperature_2m_max").optDouble(0, 0.0);
            double tmin = daily.getJSONArray("temperature_2m_min").optDouble(0, 0.0);
            double currentTemperature = (tmax + tmin) / 2.0;

            // 2) Strompreis (EUR/MWh) für die nächsten 24h via energy-charts
            long nowUnix = System.currentTimeMillis() / 1000L;
            long oneDayLater = nowUnix + 24L * 3600L;
            String priceUrl = String.format(
                    "https://api.energy-charts.info/price?bzn=DE-LU&start=%d&end=%d",
                    nowUnix, oneDayLater
            );
            String priceJson = fetchElectricityPriceData(priceUrl);
            org.json.JSONObject priceObj = new org.json.JSONObject(priceJson);
            org.json.JSONArray priceArray = priceObj.getJSONArray("price");
            double sumPrice = 0.0;
            for (int i = 0; i < priceArray.length(); i++) {
                sumPrice += priceArray.getDouble(i);
            }
            double avgPriceMWh = (priceArray.length() > 0) ? (sumPrice / priceArray.length()) : 0.0;
            // Umrechnung in EUR/kWh
            double avgPriceKWh = avgPriceMWh / 1000.0;

            // 3) Gesamtmenge im Lager
            int totalQuantity = getTotalWarehouseQuantity(); // SELECT SUM(quantity) FROM products

            // 4) pBasis, kP, grundkosten => Stromverbrauch P
            double pBasis = 1000.0;
            double kP = 7000.0;
            double grundkosten = 1000.0;
            double P;
            if (currentTemperature > 0.0) {
                P = pBasis + kP * (1.0 / currentTemperature);
            } else {
                P = pBasis + kP * Math.abs(currentTemperature);
            }

            double stromkosten = P * avgPriceKWh;
            double betriebskosten = stromkosten + grundkosten;

            // 5) costPerItem
            if (totalQuantity > 0) {
                return betriebskosten / totalQuantity;
            } else {
                // Fallback, falls totalQuantity=0
                return betriebskosten;
            }
        }


    }
