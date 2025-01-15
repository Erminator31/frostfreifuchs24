    package com.example.fff;

    import com.example.fff.api.ProductManager;
    import com.example.fff.api.WarenausgangManager;
    import com.example.fff.api.WareneingangManager;
    import com.example.fff.databse.PostgresDBProductManagement;
    import com.example.fff.databse.PostgresDBWarenausgangManagement;
    import com.example.fff.databse.PostgresDBWareneingangManagement;
    import com.example.fff.model.*;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.scheduling.annotation.EnableScheduling;
    import org.springframework.web.bind.annotation.*;

    import java.sql.Connection;
    import java.sql.SQLException;
    import java.sql.Timestamp;
    import java.time.LocalDate;
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
     *
     * Fields:
     * productManager - interface for managing products
     * warenausgangManager - manager for warenausgangs
     * wareneingangManager - manager for wareneingangs
     * LOGGER - Logger for logging messages
     *
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
        @Autowired
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
                @RequestParam(name = "year", required = false) Integer year,
                @RequestParam(name = "month", required = false) Integer month) {
            try {
                LOGGER.log(Level.INFO, "Creating historical warenausgang data in chunks.");

                // Make sure we have at least 3 products available:
                ensureProductsExist();

                // If the caller didn't provide year or month, or provided invalid values, pick smaller defaults.
                if (year == null || year < 2020) {
                    year = 2024;
                }
                if (month == null || month < 1 || month > 12) {
                    month = 1;
                }

                // We'll just generate one month’s worth of data at a time.
                LocalDate startOfMonth = LocalDate.of(year, month, 1);
                LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

                // You can tweak how many Warenausgänge to generate per month
                int ausgaengeProMonat = 30;

                // Process the chunk
                generateWarenausgaengeForDateRange(startOfMonth, endOfMonth, ausgaengeProMonat);

                String successMsg = String.format(
                        "Historical data generated successfully for %d-%02d (Warenausgänge: %d).",
                        year, month, ausgaengeProMonat
                );
                return ResponseEntity.ok(successMsg);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error generating historical data: " + e.getMessage());
            }
        }

        private void generateWarenausgaengeForDateRange(
                LocalDate startDate,
                LocalDate endDate,
                int ausgaengeProMonat
        ) throws Exception {

            // Move "current" to the start of the given month (or date range)
            LocalDate current = startDate;

            while (!current.isAfter(endDate)) {
                int m = current.getMonthValue();
                double p1, p2, p3; // Seasonal probabilities
                if (m == 12 || m == 1 || m == 2) {
                    // Winter
                    p1 = 0.4; p2 = 0.2; p3 = 0.4;
                } else if (m >= 3 && m <= 5) {
                    // Spring
                    p1 = 0.3; p2 = 0.5; p3 = 0.2;
                } else if (m >= 6 && m <= 8) {
                    // Summer
                    p1 = 0.1; p2 = 0.7; p3 = 0.2;
                } else {
                    // Autumn (9,10,11)
                    p1 = 0.3; p2 = 0.3; p3 = 0.4;
                }

                int lengthOfMonth = current.lengthOfMonth();

                // Generate X Warenausgänge for this month
                for (int i = 0; i < ausgaengeProMonat; i++) {
                    // Each Warenausgang has 2 randomly chosen product items
                    List<WarenausgangItem> items = new ArrayList<>();
                    items.add(generateWarenausgangItemWithSeason(p1, p2, p3));
                    items.add(generateWarenausgangItemWithSeason(p1, p2, p3));

                    // Pick a random day in [1..lengthOfMonth]
                    int randomDay = ThreadLocalRandom.current().nextInt(1, lengthOfMonth + 1);
                    LocalDate randomDate = current.withDayOfMonth(randomDay);

                    // Random hour [8..17], random minute [0..59]
                    int randomHour = ThreadLocalRandom.current().nextInt(8, 18);
                    int randomMinute = ThreadLocalRandom.current().nextInt(0, 60);

                    LocalDateTime warenausgangDateTime = LocalDateTime.of(
                            randomDate.getYear(),
                            randomDate.getMonthValue(),
                            randomDate.getDayOfMonth(),
                            randomHour,
                            randomMinute
                    );
                    Timestamp warenausgangTimestamp = Timestamp.valueOf(warenausgangDateTime);

                    // Create the Warenausgang
                    Warenausgang createdWarenausgang = warenausgangManager
                            .createWarenausgang(items, warenausgangTimestamp);

                    // Check if the product quantity is below reorderPoint. If so, create Wareneingang
                    for (WarenausgangItem item : items) {
                        Product updatedProduct = getProductById(item.getProductId());
                        if (updatedProduct != null
                                && updatedProduct.getProductQuantity() < updatedProduct.getReorderPoint()) {

                            WareneingangItem wareneingangItem = new WareneingangItem(
                                    updatedProduct.getProductId(),
                                    updatedProduct.getReorderQuantity()
                            );
                            // Create Wareneingang with the same timestamp to replenish
                            wareneingangManager.createWareneingang(
                                    Collections.singletonList(wareneingangItem),
                                    warenausgangTimestamp
                            );
                        }
                    }
                }
                // Move on to next month if you want multiple months in the same call
                current = current.plusMonths(1);
            }
        }


        private Product getProductById(int productId) {
            try {
                return productManager.readProductById(productId);
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
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

            // Menge zwischen 5 und 100
            int quantity = 5 + ThreadLocalRandom.current().nextInt(0, 100);
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
                productManager.addProduct("Klaus Summer", "Ohne Frostschutz", 1000);
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
        public ResponseEntity<?> createWareneingang(@RequestBody Wareneingang wareneingangRequest,
                                                    @RequestParam(value = "wareneingangDate", required = false) String wareneingangDateStr) {
            LOGGER.log(Level.INFO, "Creating wareneingang.");
            try {
                Timestamp wareneingangDate = null;
                if (wareneingangDateStr != null && !wareneingangDateStr.trim().isEmpty()) {
                    wareneingangDate = Timestamp.valueOf(wareneingangDateStr);
                }

                Wareneingang createdWareneingang;
                if (wareneingangDate == null) {
                    createdWareneingang = wareneingangManager.createWareneingang(wareneingangRequest.getItems());
                } else {
                    createdWareneingang = wareneingangManager.createWareneingang(wareneingangRequest.getItems(), wareneingangDate);
                }

                return ResponseEntity.ok(createdWareneingang);
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

            try {
                // 1) Aktuelle Weights aus DB laden
                ForecastWeights currentWeights = productManager.getForecastWeights();
                double alpha = currentWeights.getAlpha();
                double beta  = currentWeights.getBeta();
                double gamma = currentWeights.getGamma();

                // 2) Falls im Request neue Werte übergeben wurden, DB updaten
                boolean changed = false;
                if (alphaParam != null && !alphaParam.equals(alpha)) {
                    alpha = alphaParam;
                    changed = true;
                }
                if (betaParam != null && !betaParam.equals(beta)) {
                    beta = betaParam;
                    changed = true;
                }
                if (gammaParam != null && !gammaParam.equals(gamma)) {
                    gamma = gammaParam;
                    changed = true;
                }
                if (changed) {
                    productManager.updateForecastWeights(alpha, beta, gamma);
                    LOGGER.log(Level.INFO, "Forecast weights updated in DB: alpha=" + alpha
                            + ", beta=" + beta + ", gamma=" + gamma);
                }

                // 3) Wetterdaten abrufen
                String apiUrl = "https://api.open-meteo.com/v1/forecast?"
                        + "latitude=49.68&longitude=9.18"
                        + "&daily=temperature_2m_max,temperature_2m_min"
                        + "&timezone=Europe/Berlin"
                        + "&forecast_days=" + forecastDays;

                String weatherJson = fetchWeatherData(apiUrl);

                // 4) JSON parsen
                org.json.JSONObject json = new org.json.JSONObject(weatherJson);
                org.json.JSONObject daily = json.getJSONObject("daily");
                org.json.JSONArray timeArray = daily.getJSONArray("time");
                org.json.JSONArray tempMaxArray = daily.getJSONArray("temperature_2m_max");
                org.json.JSONArray tempMinArray = daily.getJSONArray("temperature_2m_min");

                // 5) Tagesdaten sammeln
                List<DailyTemperature> dailyTemperatures = new ArrayList<>();
                // Wir bauen uns auch gleich eine List<String> fuer das Frontend-Datum
                List<String> dateList = new ArrayList<>();

                // Optional: Formatter, um "2025-01-12" in "12.01.2025" zu konvertieren
                DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd. MM. yyyy");

                for (int i = 0; i < timeArray.length(); i++) {
                    String dateString = timeArray.getString(i);
                    double tmax = tempMaxArray.optDouble(i, 0.0);
                    double tmin = tempMinArray.optDouble(i, 0.0);
                    double avgTemp = (tmax + tmin) / 2.0;

                    dailyTemperatures.add(new DailyTemperature(dateString, avgTemp));

                    // Konvertieren in ein schöneres Datumsformat
                    // (z.B. "12.01.2025")
                    LocalDate parsed = LocalDate.parse(dateString, inputFmt);
                    String formattedDate = parsed.format(outputFmt);
                    dateList.add(formattedDate);
                }

                // 6) Forecast pro Produkt berechnen
                List<Product> allProducts = productManager.readProducts(null, null);

                // Hier speichern wir am Ende: data[productName] -> ArrayList<Double> (Forecast-Werte)
                Map<String, List<Double>> dataMap = new LinkedHashMap<>();

                try (Connection conn = PostgresDBWarenausgangManagement.getInstance()
                        .getDataSource()
                        .getConnection())
                {
                    for (Product product : allProducts) {
                        // Einfache Lesbarkeit:
                        int pid = product.getProductId();
                        String pName = product.getProductName();

                        // Historischer 7-Tage-Durchschnitt
                        double historicalAvg = warenausgangManager.calculateAverageDailyDemand(pid, conn);
                        LOGGER.log(Level.INFO, "historicalavg=" + historicalAvg);

                        if (historicalAvg <= 0) {
                            historicalAvg = 5.0;
                        }

                        List<Double> dailyForecastValues = new ArrayList<>();
                        for (DailyTemperature dt : dailyTemperatures) {
                            double temp = dt.avgTemp();
                            double weatherFactor = getWeatherFactor(pid, temp);
                            double seasonFactor = getSeasonFactor(pid, dt.dateString());

                            // Multiplikative Anwendung von alpha, beta, gamma
                            double forecastForDay = (alpha * historicalAvg)
                                    * (beta * weatherFactor
                                    + gamma * (1.0 + seasonFactor));
                            dailyForecastValues.add(forecastForDay);
                        }

                        // Legen wir in dataMap ab: Key=ProduktName, Value=List<Double> Forecast
                        dataMap.put(pName, dailyForecastValues);

                        // -----------------------------------------
                        // Reorder- und Wareneingangslogik:
                        // -----------------------------------------
                        // Nächste 7 Tage summieren
                        double sum7 = 0.0;
                        int limit7 = Math.min(7, dailyForecastValues.size());
                        for (int i = 0; i < limit7; i++) {
                            sum7 += dailyForecastValues.get(i);
                        }
                        int newDailyDemand = (int)Math.round(sum7 / limit7);

                        // Nächste 3 Tage summieren
                        double sum3 = 0.0;
                        int limit3 = Math.min(3, dailyForecastValues.size());
                        for (int i = 0; i < limit3; i++) {
                            sum3 += dailyForecastValues.get(i);
                        }
                        int newReorderPoint = (int)Math.round(sum3);

                        // reorder_quantity = Summe der nächsten 7 Tage
                        int reorderQty = (int)Math.round(sum7);

                        // DB-Update
                        productManager.updateProductForecastValues(product, newDailyDemand, newReorderPoint, reorderQty);

                        // Falls quantity < reorderPoint, automatischer Wareneingang
                        Product updatedP = productManager.readProductById(pid);
                        if (updatedP != null && updatedP.getProductQuantity() < updatedP.getReorderPoint()) {
                            WareneingangItem item = new WareneingangItem(pid, updatedP.getReorderQuantity());
                            wareneingangManager.createWareneingang(Collections.singletonList(item));
                            LOGGER.log(Level.INFO,
                                    "Automatische Nachbestellung für ProductID=" + pid
                                            + " mit Menge=" + updatedP.getReorderQuantity());
                        }
                    }
                }

                // 7) Antwort-Objekt bauen:
                // Wir wollen:
                // {
                //   alphaUsed: x,
                //   betaUsed: y,
                //   gammaUsed: z,
                //   dates: [ ... ],
                //   data: {
                //       "Klaus Winter": [... Forecast-Werte ...],
                //       "Klaus Summer": [...],
                //       ...
                //   }
                // }

                Map<String, Object> result = new HashMap<>();
                result.put("alphaUsed", alpha);
                result.put("betaUsed", beta);
                result.put("gammaUsed", gamma);
                result.put("dates", dateList);
                result.put("data", dataMap);

                return ResponseEntity.ok(result);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler im /forecast Endpoint: " + e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler bei /forecast: " + e.getMessage());
            }
        }


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
            // Falls < -5 Grad
            if (temperature < -5) {
                return switch (productId) {
                    case 1 -> 0.7;
                    case 2 -> 0.2;
                    case 3 -> 1.0;
                    default -> 1.0; // Fallback
                };
            }
            // Falls < 4 Grad
            else if (temperature < 4) {
                return switch (productId) {
                    case 1 -> 1.0;
                    case 2 -> 0.2;
                    case 3 -> 0.5;
                    default -> 1.0;
                };
            }
            // Falls >= 4 Grad
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
            double val = -(Math.cos(Math.PI / 6.0 * x) + 1.2) * 0.4;
            return Math.abs(val);
        }

        private double seasonFactorProduct2(int x) {
            // f(x) = | - ( cos(π/6 * x) - 1.2 ) * 0.4 |
            double val = -(Math.cos(Math.PI / 6.0 * x) - 1.2) * 0.4;
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
                if (fromDateStr != null && !fromDateStr.isEmpty()) {
                    fromTimestamp = Timestamp.valueOf(fromDateStr + " 00:00:00");
                }
                if (toDateStr != null && !toDateStr.isEmpty()) {
                    toTimestamp = Timestamp.valueOf(toDateStr + " 23:59:59");
                }

                // Abruf der Statistikdaten aus dem Manager
                List<TagesStatistik> statistik = warenausgangManager.getWarenausgaengeProTag(fromTimestamp, toTimestamp);

                if (statistik.isEmpty()) {
                    return ResponseEntity.noContent().build();
                }

                // Separiere Daten in zwei Listen: dates und data
                List<String> dates = new ArrayList<>();
                List<Integer> data = new ArrayList<>();
                DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd. MM. yyyy");

                for (TagesStatistik ts : statistik) {
                    // Konvertiere das Datum ins gewünschte Format
                    LocalDate parsedDate = LocalDate.parse(ts.getDatum(), inputFmt);
                    dates.add(parsedDate.format(outputFmt));

                    data.add(ts.getAnzahl());
                }

                // Erstelle die Antwortstruktur
                Map<String, Object> response = new HashMap<>();
                response.put("dates", dates);
                response.put("data", data);

                return ResponseEntity.ok(response);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler beim Abrufen der Tagesstatistik: " + e.getMessage());
            }
        }


    }
