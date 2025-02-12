package com.example.fff;

import com.example.fff.Services.CalculationService;
import com.example.fff.Services.HistoryService;
import com.example.fff.api.ProductManager;
import com.example.fff.api.WarenausgangManager;
import com.example.fff.api.WareneingangManager;
import com.example.fff.databse.PostgresDBProductManagement;
import com.example.fff.databse.PostgresDBWarenausgangManagement;
import com.example.fff.databse.PostgresDBWareneingangManagement;
import com.example.fff.model.*;
import com.example.fff.Services.WeatherService;
import com.example.fff.Services.CalculationService;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@EnableScheduling
@RequestMapping("/api")
public class MappingController {

    ProductManager productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
    WarenausgangManager warenausgangManager = PostgresDBWarenausgangManagement.getInstance();
    WareneingangManager wareneingangManager = PostgresDBWareneingangManagement.getInstance();
    @Autowired
    private CalculationService calculationService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private WeatherService weatherService;


    private static final Logger LOGGER = Logger.getLogger(MappingController.class.getName());



    /**
     * Handles the GET request to the "/auth" endpoint.
     *
     * Logs the incoming request and processes the specified name parameter.
     *
     * @param name the name parameter provided in the request. If not provided, defaults to "Name".
     * @return a string response indicating the result of the operation, always returning "ok".
     */
    @GetMapping("/auth")
    public String getInfo(@RequestParam(value = "name", defaultValue = "Name") String name) {
        LOGGER.log(Level.INFO, "MappingController auth " + name);
        return "ok";
    }

    /**
     * Creates a product table in the database.
     *
     * This method maps to the "/create-products-table" endpoint and invokes the
     * {@code createProductTable} method from the {@code productManager} to
     * initialize the required product table infrastructure in the database.
     *
     * @return a String indicating the success of the operation ("ok").
     * @throws Exception if an error occurs during the table creation process.
     */
    @GetMapping("/create-products-table")
    public String createProductTable() throws Exception {
        LOGGER.log(Level.INFO, "MappingController create-product-table ");
        productManager.createProductTable();
        return "ok";
    }

    /**
     * Endpoint to trigger the creation of the Warenausgang table in the database.
     * This method uses the warenausgangManager to perform the operation and
     * logs the process for tracking purposes.
     *
     * @return A confirmation string "ok" indicating the table creation operation was initiated successfully.
     * @throws Exception if an error occurs during the table creation process.
     */
    @GetMapping("/create-warenausgang-table")
    public String createWarenausgangTable() throws Exception {
        LOGGER.log(Level.INFO, "MappingController create-warenausgang-table ");
        warenausgangManager.createWarenausgangTable();
        return "ok";
    }

    /**
     * Handles the creation of the "WarenausgangItem" table by delegating the task to the
     * WarenausgangManager. This endpoint is designed to be invoked via an HTTP GET request.
     *
     * @return A string response indicating the success of the operation, typically "ok".
     * @throws Exception If an error occurs during the table creation process.
     */
    @GetMapping("/create-warenausgangitem-table")
    public String createWarenausgangItemTable() throws Exception {
        LOGGER.log(Level.INFO, "MappingController create-warenausgangitem-table ");
        warenausgangManager.createWarenausgangItemTable();
        return "ok";
    }

    /**
     * Adds or updates a product in the system.
     *
     * @param product The product object containing details such as product name, type, and quantity.
     * @return A ResponseEntity containing a success message if the operation is successful,
     *         or an error message with a BAD_REQUEST status if an exception occurs.
     */
    @PostMapping(path = "/product/add", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<?> addProduct(@RequestBody Product product) {
        LOGGER.log(Level.INFO, "MappingController POST /product/add " + product.getProductName());
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
     * Retrieves a list of products based on optional query parameters for filtering by product name and type.
     *
     * @param productName An optional parameter to filter products by their name. If null or not provided, no filtering by name is applied.
     * @param productType An optional parameter to filter products by their type. If null or not provided, no filtering by type is applied.
     * @return A ResponseEntity containing a list of products that match the filter criteria. Returns a 204 No Content response if no matching products are found.
     */
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "productType", required = false) String productType) {
        LOGGER.log(Level.INFO, "MappingController /api/products");
        List<Product> products = productManager.readProducts(productName, productType);
        if (products.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(products);
    }

    /**
     * Deletes a product with the specified ID from the system.
     *
     * @param productId the unique ID of the product to be removed.
     * @return a ResponseEntity containing a success message if the product
     *         is removed successfully, a not found response if the product
     *         does not exist, or an internal server error message if an exception occurs.
     */
    @DeleteMapping("/product/delete/{id}")
    public ResponseEntity<?> removeProduct(@PathVariable("id") int productId) {
        LOGGER.log(Level.INFO, "MappingController DELETE /product/" + productId);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing product: " + e.getMessage());
        }
    }

    /**
     * Deletes the "products" table from the database.
     * This action is performed by invoking the deleteProductsTable method of the ProductManager.
     *
     * @return a ResponseEntity containing a success message upon successful deletion or
     *         an error message with a 500 Internal Server Error status if an exception is thrown.
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
     * Deletes the "Warenausgang" table from the database by invoking the corresponding method
     * in the WarenausgangManager.
     *
     * @return ResponseEntity containing a success message if the table is deleted successfully
     * or an error message with HTTP status INTERNAL_SERVER_ERROR in case of an exception.
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

    /**
     * Deletes the "WarenausgangItem" table in the database.
     *
     * This method maps to the "/delete-warenausgangitem-table" endpoint. It invokes the
     * {@code deleteWarenausgangItemsTable()} method of the {@code warenausgangManager} to
     * perform the deletion. Upon successful operation, a success message is returned.
     * In case of an exception, an error message with HTTP status INTERNAL_SERVER_ERROR
     * is returned.
     *
     * @return A ResponseEntity containing a success message if the table is deleted successfully,
     *         or an error message with HTTP status INTERNAL_SERVER_ERROR if an exception occurs.
     */
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

    /**
     * Creates a new Warenausgang entry using the provided request data and an optional date.
     *
     * @param warenausgangRequest the Warenausgang request body containing the details of the items to be processed
     * @param warenausgangDateStr an optional string representing the timestamp for the Warenausgang; if not provided or invalid, current date will be used
     * @return ResponseEntity containing the created Warenausgang object on success or an error message on failure
     */
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

    /**
     * Retrieves a specific Warenausgang entity based on the given ID.
     *
     * @param warenausgangId the unique identifier of the Warenausgang to retrieve
     * @return a ResponseEntity containing the Warenausgang object if found, a NOT_FOUND status if it does not exist,
     *         or an INTERNAL_SERVER_ERROR status with a message in case of an exception
     */
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

    /**
     * Retrieves all warenausgaenge (goods issues) within an optional date range.
     *
     * @param fromDateStr the starting date of the range in the format "yyyy-MM-dd". If null or empty, no lower bound is applied.
     * @param toDateStr the ending date of the range in the format "yyyy-MM-dd". If null or empty, no upper bound is applied.
     * @return a ResponseEntity containing a list of Warenausgang objects if any exist within the given date range,
     *         a no-content response if none exist, or an error message with HTTP status 500 in case of failure.
     */
    @GetMapping("/warenausgaenge")
    public ResponseEntity<?> getAllWarenausgaenge(
            @RequestParam(value = "from", required = false) String fromDateStr,
            @RequestParam(value = "to", required = false) String toDateStr) {
        try {
            Timestamp fromTimestamp = null;
            Timestamp toTimestamp = null;
            if (fromDateStr != null && !fromDateStr.isEmpty()) {
                fromTimestamp = Timestamp.valueOf(fromDateStr + " 00:00:00");
            }
            if (toDateStr != null && !toDateStr.isEmpty()) {
                toTimestamp = Timestamp.valueOf(toDateStr + " 23:59:59");
            }
            List<Warenausgang> warenausgaenge = warenausgangManager.getWarenausgaenge(fromTimestamp, toTimestamp);
            if (warenausgaenge.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(warenausgaenge);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving warenausgaenge: " + e.getMessage());
        }
    }

    /**
     * Generates historical day-by-day data for a specific month and year.
     * Ensures product data exists before generating the historical data.
     * Handles special cases for specific date ranges as defined within the method.
     *
     * @param year The year for which the historical data is to be generated. Must not be null.
     * @param month The month for which the historical data is to be generated. Must not be null.
     * @return A ResponseEntity object containing a success or error message depending on the operation's outcome.
     */
    @GetMapping("/generate-history")
    public ResponseEntity<String> generateHistoricalData(
            @RequestParam(name = "year", required = true) Integer year,
            @RequestParam(name = "month", required = true) Integer month) {
        try {
            LOGGER.log(Level.INFO, "Creating historical warenausgang data (day-by-day).");
            historyService.ensureProductsExist();
            LocalDate startOfMonth = LocalDate.of(year, month, 1);
            LocalDate endOfMonth   = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
            if (year == 2025 && month == 2) {
                startOfMonth = LocalDate.of(2025, 2, 1);
                endOfMonth   = LocalDate.of(2025, 2, 2);
            }
            historyService.generateWarenausgaengeForDateRange(startOfMonth, endOfMonth);
            String successMsg = String.format("Historical data (day-by-day) generated successfully for %d-%02d.", year, month);
            return ResponseEntity.ok(successMsg);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating historical data: " + e.getMessage());
        }
    }



    /**
     * Updates the daily demand for products by invoking the update logic
     * from the productManager. This method is exposed as a GET API endpoint.
     *
     * @return ResponseEntity containing a success message if the update is
     *         successful, or an error message with a status of INTERNAL_SERVER_ERROR
     *         if an exception occurs during the update process.
     */
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

    /**
     * Retrieves a list of all wareneingaenge (goods receipts).
     * This method fetches all available wareneingaenge from the database
     * and returns them in the response.
     *
     * @return a ResponseEntity containing the list of wareneingaenge if found,
     *         a no-content response if the list is empty, or an error message
     *         if an exception occurs.
     */
    @GetMapping("/wareneingaenge")
    public ResponseEntity<?> getAllWareneingaenge() {
        try {
            List<Wareneingang> eingange = wareneingangManager.getAllWareneingaenge();
            if (eingange.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(eingange);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving wareneingaenge: " + e.getMessage());
        }
    }

    /**
     * Endpoint for creating the "Wareneingang" table. This method calls the
     * wareneingangManager to handle the creation of the required table.
     * Logs the operation and handles potential exceptions.
     *
     * @return ResponseEntity containing a success message if the table is created successfully,
     *         or an error message with an appropriate HTTP status in case of failure.
     */
    @GetMapping("/create-wareneingang-table")
    public ResponseEntity<String> createWareneingangTable() {
        LOGGER.log(Level.INFO, "MappingController create-wareneingang-table ");
        try {
            wareneingangManager.createWareneingangTable();
            return ResponseEntity.ok("Wareneingang table created successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating wareneingang table: " + e.getMessage());
        }
    }

    /**
     * Endpoint to create the Wareneingang items table.
     *
     * This method handles an HTTP GET request for creating the Wareneingang items
     * table in the database. It delegates the creation process to the
     * WareneingangManager. In case of success, a success message is returned.
     * If an error occurs during the creation process, an error message is
     * returned along with an HTTP status indicating an internal server error.
     *
     * @return ResponseEntity containing a success message if the table is created
     *         successfully, or an error message and HTTP status INTERNAL_SERVER_ERROR
     *         in case of failure.
     */
    @GetMapping("/create-wareneingangitem-table")
    public ResponseEntity<String> createWareneingangItemTable() {
        LOGGER.log(Level.INFO, "MappingController create-wareneingangitem-table ");
        try {
            wareneingangManager.createWareneingangItemTable();
            return ResponseEntity.ok("Wareneingang items table created successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating wareneingangitems table: " + e.getMessage());
        }
    }

    /**
     * Deletes the wareneingang table by invoking the corresponding method in the wareneingangManager.
     * Provides response with success or error message based on the outcome.
     *
     * @return ResponseEntity containing a success message if the operation is successful or an error message
     *         along with the HTTP status in case of failure.
     */
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

    /**
     * Deletes the "wareneingang items" table by invoking the delete operation
     * within the wareneingangManager. Responds with a success message if the
     * operation is successful, or an error message if an exception occurs.
     *
     * @return ResponseEntity containing a success message if the deletion is
     *         successful or an error message with internal server error status
     *         if an exception is thrown during the operation.
     */
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

    /**
     * Handles the creation of a manual Wareneingang entry.
     * The Wareneingang can be created with or without a specified date.
     *
     * @param wareneingangRequest The Wareneingang object containing the details of the items to be added.
     * @param wareneingangDateStr An optional string representing the date and time of the Wareneingang.
     *                            If not provided or empty, the current timestamp will be used.
     * @return A ResponseEntity containing the created Wareneingang object if successful,
     *         or an error message if the creation fails.
     */
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

    /**
     * Handles the forecast endpoint request by calculating demand forecasts for products based on
     * weather data and historical data. Updates forecast weights if provided as input parameters
     * and retrieves weather information from a third-party API. The method generates demand projections
     * for each product and updates necessary inventory management metrics.
     *
     * @param forecastDays the number of days for which the forecast is to be calculated;
     *                     defaults to 14 if not provided
     * @param alphaParam   the alpha weight for forecasting calculations; overrides default
     *                     or existing value if provided
     * @param betaParam    the beta weight for forecasting calculations; overrides default
     *                     or existing value if provided
     * @param gammaParam   the gamma weight for forecasting calculations; overrides default
     *                     or existing value if provided
     * @return a {@code ResponseEntity} object containing either the forecast results with metadata
     *         on used weights, forecasted dates, and the calculated data, or an error message
     *         in case of failures
     */
    @GetMapping("/forecast")
    public ResponseEntity<?> getForecast(
            @RequestParam(value = "forecastDays", defaultValue = "14") int forecastDays,
            @RequestParam(value = "alpha", required = false) Double alphaParam,
            @RequestParam(value = "beta", required = false) Double betaParam,
            @RequestParam(value = "gamma", required = false) Double gammaParam
    ) {
        LOGGER.log(Level.INFO, "Received /forecast request mit forecastDays=" + forecastDays);
        try (Connection conn = PostgresDBWarenausgangManagement.getInstance()
                .getDataSource().getConnection()) {

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

            String apiUrl = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=49.3536"
                    + "&longitude=9.1511"
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                    + "&forecast_days=" + forecastDays;
            String weatherJson = weatherService.fetchWeatherData(apiUrl);
            JSONObject json = new JSONObject(weatherJson);
            JSONObject daily = json.getJSONObject("daily");
            JSONArray timeArray     = daily.getJSONArray("time");
            JSONArray tempMaxArray  = daily.getJSONArray("temperature_2m_max");
            JSONArray tempMinArray  = daily.getJSONArray("temperature_2m_min");
            JSONArray precProbArray = daily.getJSONArray("precipitation_probability_max");

            List<Product> allProducts = productManager.readProducts(null, null);
            List<String> dateList = new ArrayList<>();
            DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd. MM. yyyy");

            Map<String, List<Double>> dataMap = new LinkedHashMap<>();
            for (Product p : allProducts) {
                dataMap.put(p.getProductName(), new ArrayList<>());
            }

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

                List<ProductWFactor> wFactors = new ArrayList<>();
                for (Product product : allProducts) {
                    double wf = CalculationService.getWeatherFactor(product.getProductId(), avgTemp);
                    wFactors.add(new ProductWFactor(product, wf));
                }
                wFactors.sort((a,b) -> Double.compare(b.weatherFactor(), a.weatherFactor()));
                Set<Integer> top2ProductIds = new HashSet<>();
                if (precipProb > 50.0 && wFactors.size() >= 2) {
                    top2ProductIds.add(wFactors.get(0).product().getProductId());
                    top2ProductIds.add(wFactors.get(1).product().getProductId());
                }

                for (Product product : allProducts) {
                    double historicalAvg = warenausgangManager.calculateSameDayHistoricalAverage(
                            product.getProductId(), parsedDate, 5, conn
                    );
                    if (historicalAvg <= 0) {
                        historicalAvg = 5.0;
                    }
                    double weatherFactor = CalculationService.getWeatherFactor(product.getProductId(), avgTemp);
                    double seasonFactor  = CalculationService.getSeasonFactor(product.getProductId(), dateString);
                    double base = (alpha * historicalAvg)
                            * ((beta * weatherFactor + (gamma * (1.0 + seasonFactor))) / 2.0);
                    if (top2ProductIds.contains(product.getProductId())) {
                        base *= 1.1;
                    }
                    if (isGermanHolidayOrWeekend(parsedDate)) {
                        double reduction = 0.10 + (Math.random() * 0.05);
                        base *= (1.0 - reduction);
                    }
                    double forecastForDay = base;
                    dataMap.get(product.getProductName()).add(forecastForDay);
                }
            }

            double costPerItem = calculationService.getCostPerItem();
            double orderCost = 25.0;
            LocalDate lastYearStart = LocalDate.now().minusYears(1).withDayOfYear(1);
            LocalDate lastYearEnd   = LocalDate.now().minusYears(1).withDayOfYear(lastYearStart.lengthOfYear());

            for (Product product : allProducts) {
                List<Double> fcValues = dataMap.get(product.getProductName());
                if (fcValues == null || fcValues.isEmpty()) {
                    continue;
                }
                double sum7 = 0.0;
                int limit7 = Math.min(7, fcValues.size());
                for (int i = 0; i < limit7; i++) {
                    sum7 += fcValues.get(i);
                }
                int newDailyDemand = (int) Math.round(sum7 / limit7);

                double sum3 = 0.0;
                int limit3 = Math.min(3, fcValues.size());
                for (int i = 0; i < limit3; i++) {
                    sum3 += fcValues.get(i);
                }
                int newReorderPoint = (int) Math.round(sum3);

                double annualDemand = warenausgangManager.calculateDemandInPeriod(
                        product.getProductId(),
                        lastYearStart, lastYearEnd,
                        conn
                );
                if (annualDemand < 1) {
                    annualDemand = 50.0;
                }
                LOGGER.log(Level.INFO, "ordercost: " + orderCost + " annualdemand: " + annualDemand
                        + " costPerItem " + costPerItem + " productname: " + product.getProductName());
                double eoq = Math.sqrt((2.0 * orderCost * annualDemand) / costPerItem);
                int newReorderQuantity = (int) Math.round(eoq);
                productManager.updateProductForecastValues(
                        product,
                        newDailyDemand,
                        newReorderPoint,
                        newReorderQuantity
                );
            }

            Map<String, Object> result = new HashMap<>();
            result.put("alphaUsed", alpha);
            result.put("betaUsed", beta);
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

    // Record zur Unterstützung der Forecast-Berechnung
    private record ProductWFactor(Product product, double weatherFactor) {}

    /**
     * Checks if the given date is either a recognized German public holiday or falls on a weekend.
     *
     * @param date the date to be checked
     * @return true if the date is a German public holiday or a weekend (Saturday or Sunday), false otherwise
     */
    private boolean isGermanHolidayOrWeekend(LocalDate date) {
        Set<MonthDay> GERMAN_HOLIDAYS = Set.of(
                MonthDay.of(1, 1),
                MonthDay.of(1, 6),
                MonthDay.of(5, 1),
                MonthDay.of(8, 15),
                MonthDay.of(10, 3),
                MonthDay.of(10, 31),
                MonthDay.of(11, 1),
                MonthDay.of(12, 25),
                MonthDay.of(12, 26)
        );
        if (GERMAN_HOLIDAYS.contains(MonthDay.from(date))) {
            return true;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Handles the HTTP GET request to create the "forecast_weights" table in the database.
     * Delegates the table creation task to the ProductManager.
     * If the table already exists, no changes are made.
     * Logs the process and handles any exceptions that occur during execution.
     *
     * @return A ResponseEntity containing a success message when the table is created or already exists,
     *         or an error message with an HTTP status of 500 in case of failure.
     */
    @GetMapping("/create-forecast-weights-table")
    public ResponseEntity<String> createForecastWeightsTable() {
        LOGGER.log(Level.INFO, "MappingController create-forecast-weights-table invoked");
        try {
            productManager.createForecastWeightsTable();
            return ResponseEntity.ok("forecast_weights table created (or already exists).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Erstellen der forecast_weights-Tabelle: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating forecast_weights table: " + e.getMessage());
        }
    }

    /**
     * Retrieves the daily product shipment statistics within a provided date range.
     *
     * This method processes data from the given date range and organizes it into a structure
     * where each date within the range is associated with respective product shipment counts.
     * If no dates are provided, a default range from 13 days ago to today is used.
     *
     * @param fromDateStr The start date of the range in "yyyy-MM-dd" format. If not provided, it defaults to 13 days ago.
     * @param toDateStr The end date of the range in "yyyy-MM-dd" format. If not provided, it defaults to today.
     * @return A ResponseEntity containing a map with two keys:
     *         - "dates": List of formatted date strings within the specified range.
     *         - "data": A mapping of product names to lists of daily shipment counts.
     *         If an error occurs, returns an error message with HTTP status 500.
     */
    @GetMapping("/warenausgaenge/pro-tag")
    public ResponseEntity<?> getWarenausgaengeProTag(
            @RequestParam(value = "from", required = false) String fromDateStr,
            @RequestParam(value = "to", required = false) String toDateStr) {
        try {
            Timestamp fromTimestamp = null;
            Timestamp toTimestamp = null;
            DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd. MM. yyyy");
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
            List<TagesStatistik> statistik = warenausgangManager.getWarenausgaengeProTag(fromTimestamp, toTimestamp);
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
                    .body("Fehler beim Abrufen der Tagesstatistik: " + e.getMessage());
        }
    }

    /**
     * Retrieves statistical data of goods received per day within a specified date range.
     * The data includes the number of goods received for each product on each day in the range.
     * If no date range is provided, defaults to the last 14 days, including today.
     *
     * @param fromDateStr the start date of the range in "yyyy-MM-dd" format (optional). If not provided, defaults to 13 days before today.
     * @param toDateStr the end date of the range in "yyyy-MM-dd" format (optional). If not provided, defaults to today.
     * @return a ResponseEntity containing a map with dates and data, where:
     *         - "dates" is a list of formatted date strings representing the days in the range.
     *         - "data" is a map of product names to lists of integers, where each integer represents
     *           the count of goods received on a corresponding day in the date list.
     *         Returns a 204 No Content response if no data is found for the given range.
     *         Returns a 500 Internal Server Error response in case of an error.
     */
    @GetMapping("/wareneingaenge/pro-tag")
    public ResponseEntity<?> getWareneingaengeProTag(
            @RequestParam(value = "from", required = false) String fromDateStr,
            @RequestParam(value = "to", required = false) String toDateStr) {
        try {
            Timestamp fromTimestamp = null;
            Timestamp toTimestamp = null;
            DateTimeFormatter inputFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd. MM. yyyy");
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
            List<TagesStatistik> statistik = wareneingangManager.getWareneingaengeProTag(fromTimestamp, toTimestamp);
            if (statistik.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
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

    /**
     * Partially updates the details of an existing product identified by its ID.
     * Accepts a map of properties to update and applies them to the product.
     *
     * @param productId the ID of the product to be updated
     * @param updates a map containing the fields and their respective new values to be updated
     * @return ResponseEntity containing a success message if the update is successful,
     *         a NOT_FOUND status if the product does not exist, or an INTERNAL_SERVER_ERROR
     *         status if an error occurs during the update process
     */
    @PatchMapping("/products/update/{id}")
    public ResponseEntity<?> patchProduct(
            @PathVariable("id") int productId,
            @RequestBody Map<String, Object> updates) {
        LOGGER.log(Level.INFO, "Patching product with ID " + productId);
        try {
            Product existing = productManager.readProductById(productId);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
            }
            productManager.updateProductPartial(productId, updates);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Product updated successfully.");
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error patching product: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating product: " + e.getMessage());
        }
    }

    /**
     * Deletes a warenausgang identified by the given ID.
     *
     * @param warenausgangId the unique identifier of the warenausgang to be deleted
     * @return a ResponseEntity containing a success message if deleted,
     *         a not found message if the warenausgang does not exist,
     *         or an error message in case of an exception
     */
    @DeleteMapping("/warenausgang/delete/{id}")
    public ResponseEntity<?> deleteWarenausgang(@PathVariable("id") int warenausgangId) {
        LOGGER.log(Level.INFO, "Deleting warenausgang with ID " + warenausgangId);
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
            LOGGER.log(Level.SEVERE, "Error deleting warenausgang: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting warenausgang: " + e.getMessage());
        }
    }

    /**
     * Endpoint to calculate the storage costs based on weather data, electricity prices,
     * and product quantities in the warehouse. The calculation considers the current
     * temperature, electricity price, operational costs, and total warehouse quantity
     * to determine costs per item.
     *
     * @param pBasis Base power consumption value used in the calculation. Defaults to 1000 if not specified.
     * @param kP Coefficient related to temperature-dependent power consumption. Defaults to 7000 if not specified.
     * @param grundkosten Fixed operational costs for storage operations. Defaults to 1000 if not specified.
     * @return ResponseEntity containing a map with calculated results including temperature,
     *         electricity prices, total quantity, power consumption, storage costs, and cost per item.
     *         If an error occurs, a response with appropriate HTTP status and error message is returned.
     */
    @GetMapping("/lagerkosten")
    public ResponseEntity<?> getLagerkosten(
            @RequestParam(value = "pBasis", defaultValue = "1000") double pBasis,
            @RequestParam(value = "kP", defaultValue = "7000") double kP,
            @RequestParam(value = "grundkosten", defaultValue = "1000") double grundkosten
    ) {
        try {
            String weatherUrl = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=49.3536"
                    + "&longitude=9.1511"
                    + "&daily=temperature_2m_max,temperature_2m_min"
                    + "&forecast_days=1"
                    + "&timezone=Europe/Berlin";
            String weatherJson = weatherService.fetchWeatherData(weatherUrl);
            JSONObject json = new JSONObject(weatherJson);
            JSONObject daily = json.getJSONObject("daily");
            double tmax = daily.getJSONArray("temperature_2m_max").optDouble(0, 0.0);
            double tmin = daily.getJSONArray("temperature_2m_min").optDouble(0, 0.0);
            double currentTemperature = (tmax + tmin) / 2.0;

            long nowUnix = System.currentTimeMillis() / 1000L;
            long oneDayLater = nowUnix + 24L * 3600L;
            String priceUrl = String.format(
                    "https://api.energy-charts.info/price?bzn=DE-LU&start=%d&end=%d",
                    nowUnix, oneDayLater
            );
            String priceJson = weatherService.fetchElectricityPriceData(priceUrl);
            JSONObject priceObj = new JSONObject(priceJson);
            JSONArray priceArray = priceObj.getJSONArray("price");
            double sumPrice = 0.0;
            for (int i = 0; i < priceArray.length(); i++) {
                sumPrice += priceArray.getDouble(i);
            }
            double avgPriceMWh = (priceArray.length() == 0) ? 0 : (sumPrice / priceArray.length());
            double avgPriceKWh = avgPriceMWh / 1000.0;

            int totalQuantity = productManager.getTotalWarehouseQuantity();

            double P;
            if (currentTemperature > 0) {
                P = pBasis + kP * (1.0 / currentTemperature);
            } else {
                double absT = Math.abs(currentTemperature);
                P = pBasis + kP * absT;
            }
            double stromkosten = P * avgPriceKWh;
            double betriebskosten = stromkosten + grundkosten;
            double costPerItem = (totalQuantity > 0) ? betriebskosten / totalQuantity : 0.0;

            Map<String, Object> result = new HashMap<>();
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler bei Lagerkosten-Berechnung: " + e.getMessage());
        }
    }



}
