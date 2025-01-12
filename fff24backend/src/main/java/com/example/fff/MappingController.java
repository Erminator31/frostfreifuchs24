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
    import org.springframework.web.bind.annotation.*;

    import java.sql.Connection;
    import java.sql.SQLException;
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
        WareneingangManager wareneingangManager = PostgresDBWareneingangManagement.getInstance();
        @Autowired
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
                @RequestParam(value = "forecastDays", defaultValue = "14") int forecastDays
        ) {
            // Logger, damit du siehst, wann der Endpoint aufgerufen wird
            LOGGER.log(Level.INFO, "Received /forecast request mit forecastDays=" + forecastDays);

            try {
                // 1) Wetterdaten abrufen
                //    Für das Beispiel verwenden wir Java 11+ HttpClient.
                //    Du kannst auch RestTemplate oder WebClient nutzen.
                String apiUrl = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=49.68&longitude=9.18" +
                        "&daily=temperature_2m_max,temperature_2m_min" +
                        "&timezone=Europe/Berlin" +
                        "&forecast_days=" + forecastDays;  // vom User festgelegt

                // HTTP-Aufruf
                String weatherJson = fetchWeatherData(apiUrl);

                // 2) JSON parsen (z. B. via Jackson oder org.json):
                //    Wir gehen hier beispielhaft mit org.json vor.
                //    In einem echten Projekt evtl. über Jackson/DTOs besser lösen.
                org.json.JSONObject json = new org.json.JSONObject(weatherJson);
                org.json.JSONObject daily = json.getJSONObject("daily");
                org.json.JSONArray timeArray = daily.getJSONArray("time");
                org.json.JSONArray tempMaxArray = daily.getJSONArray("temperature_2m_max");
                org.json.JSONArray tempMinArray = daily.getJSONArray("temperature_2m_min");

                // Wir sammeln die Forecast-Tagesdaten (Datum + Durchschnittstemperatur)
                List<DailyTemperature> dailyTemperatures = new ArrayList<>();
                for (int i = 0; i < timeArray.length(); i++) {
                    String dateString = timeArray.getString(i);
                    double tmax = tempMaxArray.optDouble(i, 0.0);
                    double tmin = tempMinArray.optDouble(i, 0.0);
                    double avgTemp = (tmax + tmin) / 2.0; // einfacher Durchschnitt
                    dailyTemperatures.add(new DailyTemperature(dateString, avgTemp));
                }

                // 3) Pro Produkt berechnen wir nun den erwarteten Tagesbedarf:
                //    - Historische Warenausgänge (7 Tage Durchschnitt) => schon in DB,
                //      bzw. wir nutzen die vorhandene Methode `calculateAverageDailyDemand(...)`
                //    - Wetterfaktor
                //    - Saisonaler Cosinus-Faktor
                //    - Summiere Forecast für die nächsten 7 Tage => newDailyDemand
                //    - Summiere Forecast für die nächsten 3 Tage => reorderPoint
                //    - Summiere Forecast für die nächsten 7 Tage => reorderQuantity
                //    - Falls quantity < reorderPoint => Wareneingang anstoßen

                List<Product> allProducts = productManager.readProducts(null, null);
                Map<Integer, List<Double>> productDailyForecasts = new HashMap<>();

                // Verbindung für evtl. DB-Berechnungen (z. B. averageDailyDemand)
                try (Connection conn = PostgresDBWarenausgangManagement.getInstance()
                        .getDataSource()
                        .getConnection())
                {
                    for (Product product : allProducts) {
                        int pid = product.getProductId();

                        // 3.1) Historischer 7-Tage-Durchschnitt
                        double historicalAvg = warenausgangManager.calculateAverageDailyDemand(pid, conn);
                        if (historicalAvg <= 0) {
                            // Falls noch keine Daten vorhanden -> setze minimalen Wert
                            historicalAvg = 5.0;
                        }

                        // 3.2) Für jeden Forecast-Tag:
                        //      berechne wetterFaktor x saisonFaktor x historicalAvg
                        List<Double> dailyForecastValues = new ArrayList<>();
                        for (DailyTemperature dt : dailyTemperatures) {
                            double temp = dt.avgTemp();
                            double weatherFactor = getWeatherFactor(product.getProductId(), temp);
                            double seasonFactor = getSeasonFactor(product.getProductId(), dt.dateString());
                            double forecastForDay = historicalAvg * weatherFactor * (1.0 + seasonFactor);
                            dailyForecastValues.add(forecastForDay);
                        }
                        productDailyForecasts.put(pid, dailyForecastValues);

                        // 3.3) Nächste 7 Tage mitteln -> newDailyDemand
                        //      Summiere alle 7-Tage-Forecast-Werte, dann /7
                        double sum7 = 0.0;
                        int limit7 = Math.min(7, dailyForecastValues.size());
                        for (int i = 0; i < limit7; i++) {
                            sum7 += dailyForecastValues.get(i);
                        }
                        int newDailyDemand = (int)Math.round(sum7 / limit7);

                        // 3.4) reorder_point = Summe der nächsten 3 Tage (rounded)
                        double sum3 = 0.0;
                        int limit3 = Math.min(3, dailyForecastValues.size());
                        for (int i = 0; i < limit3; i++) {
                            sum3 += dailyForecastValues.get(i);
                        }
                        int newReorderPoint = (int)Math.round(sum3);

                        // 3.5) reorder_quantity = Summe der nächsten 7 Tage (gerundet)
                        int reorderQty = (int)Math.round(sum7);

                        // 3.6) Werte in DB updaten
                        productManager.updateProductForecastValues(product, newDailyDemand, newReorderPoint, reorderQty);

                        // 3.7) Prüfen, ob quantity < reorderPoint => automatischer Wareneingang
                        //      gem. Anforderung: reorder_quantity
                        Product updatedP = productManager.readProductById(pid);
                        if (updatedP != null && updatedP.getProductQuantity() < updatedP.getReorderPoint()) {
                            // Automatische Nachbestellung
                            WareneingangItem item = new WareneingangItem(pid, updatedP.getReorderQuantity());
                            wareneingangManager.createWareneingang(Collections.singletonList(item));
                            LOGGER.log(Level.INFO,
                                    "Automatische Nachbestellung für ProductID=" + pid +
                                            " mit Menge=" + updatedP.getReorderQuantity());
                        }
                    }
                }

                // 4) Fürs Frontend ausgeben: pro Tag und pro Produkt, wie viel Daily Demand erwartet wird
                //    Damit man die Forecast-Daten sieht
                Map<String, Object> result = new HashMap<>();
                result.put("forecastDays", forecastDays);
                result.put("weatherData", dailyTemperatures);
                // Bsp.: productDailyForecasts enthält pro productId eine Liste der Forecasts
                result.put("productForecasts", productDailyForecasts);

                return ResponseEntity.ok(result);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fehler im /forecast Endpoint: " + e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Fehler bei /forecast: " + e.getMessage());
            }
        }

        private String fetchWeatherData(String url) throws Exception {
            // Java 11 HttpClient
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

        private static record DailyTemperature(String dateString, double avgTemp) {}

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
                    case 1 -> 0.2;
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



    }
