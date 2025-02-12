        package com.example.fff.databse;
    
        import com.example.fff.api.ProductManager;
        import com.example.fff.model.ForecastWeights;
        import com.example.fff.model.Product;
        import org.apache.commons.dbcp.BasicDataSource;
        import org.springframework.stereotype.Service;
    
        import java.sql.*;
        import java.util.ArrayList;
        import java.util.List;
        import java.util.Map;
        import java.util.logging.Level;
        import java.util.logging.Logger;
    
        /**
         * The PostgresDBProductManagement class provides functionality for managing products
         * in a PostgreSQL database. It includes operations for creating tables, reading, adding,
         * updating, and deleting products, as well as managing forecasting parameters.
         * This class handles database connections, ensures data integrity, and supports
         * transactional operations.
         */
        @Service
        public class PostgresDBProductManagement implements ProductManager {

            String databaseURL = "jdbc:postgresql://c7u1tn6bvvsodf.cluster-czz5s0kz4scl.eu-west-1.rds.amazonaws.com:5432/d1t207hd56v54?sslmode=require";
            String username = "u3t73itv4ifknl";
            String password = "pc8d79bc3deea2ca2b99f04d14057aeb257bac911861af2c1c3f890ffecaa803c";


            BasicDataSource basicDataSource;

            @Override
            public BasicDataSource getDataSource(){
                return basicDataSource;
            }

            private static PostgresDBProductManagement postgresDBProductManagement = null;

            private static final Logger LOGGER = Logger.getLogger(PostgresDBProductManagement.class.getName());


            /**
             * Constructor for PostgresDBProductManagement class.
             * Initializes the basicDataSource with the provided database URL, username, and password.
             */
            private PostgresDBProductManagement() {
                basicDataSource = new BasicDataSource();
                basicDataSource.setUrl(databaseURL);
                basicDataSource.setUsername(username);
                basicDataSource.setPassword(password);
            }

            /**
             * Closes the provided resources including ResultSet, PreparedStatement, and Connection.
             *
             * @param rs         The ResultSet to be closed.
             * @param stmt       The PreparedStatement to be closed.
             * @param connection The Connection to be closed.
             */
            private void closeResources(ResultSet rs, PreparedStatement stmt, Connection connection) {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    LOGGER.severe("Error closing resources: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            /**
             * Retrieves the instance of PostgresDBProductManagement. If the instance does not exist, a new one is created.
             *
             * @return The instance of PostgresDBProductManagement.
             */
            public static PostgresDBProductManagement getPostgresDBProductManagement() {
                if (postgresDBProductManagement == null) {
                    postgresDBProductManagement = new PostgresDBProductManagement();
                }
                return postgresDBProductManagement;
            }

            /**
             * Creates an 'events' table in the database if it doesn't already exist.
             *
             * @throws Exception if there's any issue creating the table
             */
            @Override
            public void createProductTable() throws Exception {
                Connection connection = null;
                PreparedStatement pstmt = null;
                // CREATE TABLE Statement korrigiert und auf products angepasst
                String createTableSQL = "CREATE TABLE IF NOT EXISTS products ("
                        + "productid SERIAL PRIMARY KEY, "
                        + "productname VARCHAR(255) NOT NULL, "
                        + "producttype VARCHAR(100) NOT NULL,"
                        + "quantity INT DEFAULT NULL, "
                        + "daily_demand INT DEFAULT 500, "
                        + "reorder_point INT DEFAULT 250, " // daily_demand * 3
                        + "reorder_quantity INT DEFAULT 250" // Beispielwert, kann angepasst werden
                        + ");";

                try {
                    connection = basicDataSource.getConnection();
                    pstmt = connection.prepareStatement(createTableSQL);
                    pstmt.execute();
                } catch (SQLException e) {
                    throw new Exception("Error creating products table", e);
                } finally {
                    if (pstmt != null)
                        pstmt.close();
                    if (connection != null)
                        connection.close();
                }
            }

            /**
             * Reads a product from the database based on the provided product ID.
             *
             * @param productId The unique ID of the product to be retrieved.
             * @return The Product object representing the retrieved product, or null if no product is found with the given ID.
             * @throws SQLException If a database access error occurs.
             */
            @Override
            public Product readProductById(int productId) throws SQLException {
                String sql = "SELECT productid, productname, producttype, quantity, daily_demand, reorder_point, reorder_quantity FROM products WHERE productid = ?";

                try (Connection connection = basicDataSource.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, productId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return new Product(
                                    rs.getInt("productid"),
                                    rs.getString("productname"),
                                    rs.getString("producttype"),
                                    rs.getInt("quantity"),
                                    rs.getInt("daily_demand"),
                                    rs.getInt("reorder_point"),
                                    rs.getInt("reorder_quantity")
                            );
                        }
                    }
                }

                return null;
            }

            /**
             * Adds a product to the system with predefined default values for daily demand, reorder point, and reorder quantity.
             *
             * @param productName The name of the product to be added.
             * @param productType The type/category of the product to be added.
             * @param quantity    The quantity of the product to be added.
             * @return The added Product object with its properties set, including calculated default values.
             * @throws Exception If there is an error during the process of adding the product.
             */
            @Override
            public Product addProduct(String productName, String productType, int quantity) throws Exception {
                int initialDailyDemand = 15;
                return addProduct(productName, productType, quantity, initialDailyDemand, initialDailyDemand * 3, initialDailyDemand*14);
            }


            /**
             * Adds a product to the database with the provided parameters. If the product already exists,
             * it updates the existing product's quantity and recalculates its reorder parameters.
             * Ensures that the warehouse's total capacity of 4000 units is not exceeded.
             *
             * @param productName      The name of the product to be added or updated.
             * @param productType      The type or category of the product.
             * @param quantity         The quantity of the product to add or update.
             * @param dailyDemand      The daily demand of the product to be used for calculations.
             * @param reorderPoint     The reorder point value for the product.
             * @param reorderQuantity  The reorder quantity value for the product.
             * @return The Product object representing the added or updated product, including its calculated or updated details.
             * @throws Exception If an error occurs during the addition/updating process, including exceeding warehouse capacity or database access issues.
             */
            public Product addProduct(String productName, String productType, int quantity, int dailyDemand, int reorderPoint, int reorderQuantity) throws Exception {
                final Logger createProductLogger = Logger.getLogger("CreateProductLogger");
                createProductLogger.log(Level.INFO, "Start creating or updating product: " + productName + " with quantity " + quantity);

                Connection connection = null;
                PreparedStatement checkProductStmt = null;
                PreparedStatement insertStmt = null;
                PreparedStatement updateStmt = null;
                PreparedStatement sumStmt = null;
                ResultSet rs = null;

                try {
                    connection = basicDataSource.getConnection();
                    connection.setAutoCommit(false);

                    // Aktuelle Gesamtmenge im Lager prüfen (Maximal 4000)
                    String sumSQL = "SELECT COALESCE(SUM(quantity), 0) AS total_quantity FROM products";
                    sumStmt = connection.prepareStatement(sumSQL);
                    rs = sumStmt.executeQuery();
                    int currentTotalQuantity = 0;
                    if (rs.next()) {
                        currentTotalQuantity = rs.getInt("total_quantity");
                    }
                    rs.close();
                    sumStmt.close();

                    if (currentTotalQuantity + quantity > 4000) {
                        connection.rollback();
                        throw new Exception("Cannot add product. Adding " + quantity + " units would exceed the total warehouse capacity of 4000.");
                    }

                    // Prüfen, ob das Produkt bereits existiert
                    String checkProductSQL = "SELECT productid, quantity, daily_demand, reorder_point, reorder_quantity FROM products WHERE productname = ? AND producttype = ?";
                    checkProductStmt = connection.prepareStatement(checkProductSQL);
                    checkProductStmt.setString(1, productName);
                    checkProductStmt.setString(2, productType);
                    rs = checkProductStmt.executeQuery();

                    if (rs.next()) {
                        // Produkt existiert bereits, Menge erhöhen und Reorder-Parameter aktualisieren
                        int productId = rs.getInt("productid");
                        int currentQuantity = rs.getInt("quantity");
                        int currentDailyDemand = rs.getInt("daily_demand");
                        rs.close();
                        checkProductStmt.close();

                        int newDailyDemand = (currentDailyDemand + dailyDemand) / 2; // Durchschnittlicher täglicher Bedarf
                        int newReorderPoint = newDailyDemand * 7;
                        int newReorderQuantity = newDailyDemand * 14;

                        int newQuantity = currentQuantity + quantity;

                        String updateSQL = "UPDATE products SET quantity = ?, daily_demand = ?, reorder_point = ?, reorder_quantity = ? WHERE productid = ?";
                        updateStmt = connection.prepareStatement(updateSQL);
                        updateStmt.setInt(1, newQuantity);
                        updateStmt.setInt(2, newDailyDemand);
                        updateStmt.setInt(3, newReorderPoint);
                        updateStmt.setInt(4, newReorderQuantity);
                        updateStmt.setInt(5, productId);
                        int affectedRows = updateStmt.executeUpdate();
                        if (affectedRows == 0) {
                            connection.rollback();
                            throw new SQLException("Updating product failed, no rows affected.");
                        }

                        connection.commit();
                        return new Product(productId, productName, productType, newQuantity, newDailyDemand, newReorderPoint, newReorderQuantity);

                    } else {
                        // Produkt existiert nicht, neu anlegen
                        rs.close();
                        checkProductStmt.close();

                        String insertSQL = "INSERT INTO products (productname, producttype, quantity, daily_demand, reorder_point, reorder_quantity) VALUES (?, ?, ?, ?, ?, ?) RETURNING productid;";
                        insertStmt = connection.prepareStatement(insertSQL);
                        insertStmt.setString(1, productName);
                        insertStmt.setString(2, productType);
                        insertStmt.setInt(3, quantity);
                        insertStmt.setInt(4, dailyDemand);
                        insertStmt.setInt(5, reorderPoint);
                        insertStmt.setInt(6, reorderQuantity); // reorderQuantity = dailyDemand * 14

                        rs = insertStmt.executeQuery();
                        int generatedId = -1;
                        if (rs.next()) {
                            generatedId = rs.getInt(1);
                        } else {
                            connection.rollback();
                            throw new SQLException("Creating product failed, no ID obtained.");
                        }

                        connection.commit();
                        return new Product(generatedId, productName, productType, quantity, dailyDemand, reorderPoint, reorderQuantity);
                    }

                } catch (Exception e) {
                    if (connection != null) {
                        connection.rollback();
                    }
                    throw e;
                } finally {
                    if (rs != null) rs.close();
                    if (checkProductStmt != null) checkProductStmt.close();
                    if (insertStmt != null) insertStmt.close();
                    if (updateStmt != null) updateStmt.close();
                    if (connection != null) connection.close();
                }
            }


            /**
             * Retrieves a list of products based on the specified filters.
             *
             * @param productName The name of the product to filter by. Can be null or empty to ignore.
             * @param productType The type of the product to filter by. Can be null or empty to ignore.
             * @return A List of Product objects that match the provided filters.
             */
            @Override
            public List<Product> readProducts(String productName, String productType) {
                final Logger readProductLogger = Logger.getLogger("ReadProductLogger");
                readProductLogger.log(Level.INFO, "Start reading products with filters name=" + productName + " type=" + productType);

                List<Product> products = new ArrayList<>();

                // Dynamische Abfrage bauen
                StringBuilder queryBuilder = new StringBuilder("SELECT * FROM products");
                List<Object> parameters = new ArrayList<>();

                boolean hasNameFilter = productName != null && !productName.trim().isEmpty();
                boolean hasTypeFilter = productType != null && !productType.trim().isEmpty();

                if (hasNameFilter || hasTypeFilter) {
                    queryBuilder.append(" WHERE");
                }
                if (hasNameFilter) {
                    queryBuilder.append(" productname ILIKE ?");
                    parameters.add("%" + productName + "%");
                }
                if (hasTypeFilter) {
                    if (hasNameFilter) {
                        queryBuilder.append(" AND");
                    }
                    queryBuilder.append(" producttype ILIKE ?");
                    parameters.add("%" + productType + "%");
                }

                try (Connection connection = basicDataSource.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString())) {

                    // Parameter setzen
                    for (int i = 0; i < parameters.size(); i++) {
                        stmt.setObject(i + 1, parameters.get(i));
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            products.add(new Product(
                                    rs.getInt("productid"),
                                    rs.getString("productname"),
                                    rs.getString("producttype"),
                                    rs.getInt("quantity"),
                                    rs.getInt("daily_demand"),
                                    rs.getInt("reorder_point"),
                                    rs.getInt("reorder_quantity")

                            ));
                        }
                    }

                } catch (SQLException e) {
                    readProductLogger.log(Level.SEVERE, "Error reading products", e);
                }

                return products;
            }

            /**
             * Removes a product from the database based on the provided product ID.
             *
             * @param productId The ID of the product to be removed.
             * @return True if the product was successfully removed, false otherwise.
             */
            @Override
            public boolean removeProduct(int productId) {
                final Logger removeProductLogger = Logger.getLogger("RemoveProductLogger");
                removeProductLogger.log(Level.INFO, "Start removing product with ID: " + productId);

                Connection connection = null;
                PreparedStatement stmt = null;

                String deleteSQL = "DELETE FROM products WHERE productid = ?";

                try {
                    connection = basicDataSource.getConnection();
                    stmt = connection.prepareStatement(deleteSQL);
                    stmt.setInt(1, productId);

                    int affectedRows = stmt.executeUpdate();
                    // affectedRows sollte 1 sein, wenn genau ein Produkt entfernt wurde.
                    return affectedRows == 1;

                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    // Ressourcen freigeben
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }

                return false; // Wenn ein Fehler auftritt oder kein Produkt gefunden wurde
            }

            @Override
            public void deleteProductsTable() throws SQLException {
                Connection connection = null;
                Statement stmt = null;
                String dropTableSQL = "DROP TABLE IF EXISTS products CASCADE;";

                try {
                    connection = basicDataSource.getConnection();
                    stmt = connection.createStatement();
                    stmt.execute(dropTableSQL);
                } catch (SQLException e) {
                    throw e;
                } finally {
                    if (stmt != null)
                        stmt.close();
                    if (connection != null)
                        connection.close();
                }
            }
            // Ergänzen Sie die bestehende Klasse mit der updateDailyDemand Methode

            /**
             * Updates the daily demand, reorder point, and reorder quantity for all products in the database.
             *
             * This method retrieves all products from the database, calculates their average daily demand using
             * the PostgresDBWarenausgangManagement instance, and updates the respective fields in the database
             * for each product. The calculations for update values are based on fixed intervals:
             * - Reorder Point: 7 days of daily demand
             * - Reorder Quantity: 14 days of daily demand
             *
             * The method performs all updates in a batch process and ensures database transaction integrity
             * with manual commitment and rollback in case of errors. Logging is performed at each step to
             * provide detailed information about the process and any potential errors encountered.
             *
             * Throws an exception if any error occurs during the process, including database connection issues,
             * query execution errors, or unexpected processing failures.
             *
             * @throws Exception if an error occurs during the update process, including database access errors
             *                   or computational errors.
             */
            @Override
            public void updateDailyDemand() throws Exception {
                final Logger updateDemandLogger = Logger.getLogger("UpdateDemandLogger");
                updateDemandLogger.log(Level.INFO, "Start updating dailyDemand for all products.");

                Connection connection = null;
                PreparedStatement selectProductsStmt = null;
                PreparedStatement updateProductStmt = null;
                ResultSet rs = null;

                try {
                    connection = basicDataSource.getConnection();
                    connection.setAutoCommit(false);

                    // Alle Produkte abrufen
                    String selectProductsSQL = "SELECT productid FROM products;";
                    selectProductsStmt = connection.prepareStatement(selectProductsSQL);
                    rs = selectProductsStmt.executeQuery();

                    List<Integer> productIds = new ArrayList<>();
                    while (rs.next()) {
                        productIds.add(rs.getInt("productid"));
                    }
                    rs.close();
                    selectProductsStmt.close();

                    // Vorbereitung des Update-Statements
                    String updateProductSQL = "UPDATE products SET daily_demand = ?, reorder_point = ?, reorder_quantity = ? WHERE productid = ?;";
                    updateProductStmt = connection.prepareStatement(updateProductSQL);

                    PostgresDBWarenausgangManagement orderManager = PostgresDBWarenausgangManagement.getInstance();

                    for (int productId : productIds) {
                        double avgDailyDemand = orderManager.calculateAverageDailyDemand(productId, connection);
                        int newDailyDemand = (int) Math.round(avgDailyDemand);
                        int newReorderPoint = newDailyDemand * 7;
                        int newReorderQuantity = newDailyDemand * 14;

                        updateProductStmt.setInt(1, newDailyDemand);
                        updateProductStmt.setInt(2, newReorderPoint);
                        updateProductStmt.setInt(3, newReorderQuantity);
                        updateProductStmt.setInt(4, productId);
                        updateProductStmt.addBatch();

                        updateDemandLogger.log(Level.INFO, "Updated Product ID " + productId + ": dailyDemand=" + newDailyDemand + ", reorderPoint=" + newReorderPoint + ", reorderQuantity=" + newReorderQuantity);
                    }

                    // Batch-Update ausführen
                    updateProductStmt.executeBatch();
                    connection.commit();
                    updateDemandLogger.log(Level.INFO, "dailyDemand update completed successfully.");
                } catch (Exception e) {
                    if (connection != null) {
                        connection.rollback();
                    }
                    LOGGER.log(Level.SEVERE, "Error updating dailyDemand: " + e.getMessage(), e);
                    throw e;
                } finally {
                    if (rs != null) rs.close();
                    if (selectProductsStmt != null) selectProductsStmt.close();
                    if (updateProductStmt != null) updateProductStmt.close();
                    if (connection != null) connection.close();
                }
            }

            /**
             * Updates the forecast values of a product in the database with new values for daily demand,
             * reorder point, and reorder quantity.
             *
             * @param product The Product object representing the product to be updated. The product must exist in the database.
             * @param newDailyDemand The new daily demand value to be set for the product.
             * @param newReorderPoint The new reorder point value to be set for the product.
             * @param newReorderQuantity The new reorder quantity value to be set for the product.
             * @throws SQLException If a database access error occurs while updating the product.
             */
            @Override
            public void updateProductForecastValues(Product product,
                                                    int newDailyDemand,
                                                    int newReorderPoint,
                                                    int newReorderQuantity) throws SQLException {
                Connection connection = null;
                PreparedStatement stmt = null;
                try {
                    connection = PostgresDBProductManagement.getPostgresDBProductManagement()
                            .basicDataSource
                            .getConnection();
                    String sql = """
                    UPDATE products
                    SET daily_demand = ?,
                        reorder_point = ?,
                        reorder_quantity = ?
                    WHERE productid = ?
                """;
                    stmt = connection.prepareStatement(sql);
                    stmt.setInt(1, newDailyDemand);
                    stmt.setInt(2, newReorderPoint);
                    stmt.setInt(3, newReorderQuantity);
                    stmt.setInt(4, product.getProductId());
                    stmt.executeUpdate();
                } finally {
                    if (stmt != null) stmt.close();
                    if (connection != null) connection.close();
                }
            }

            /**
             * Retrieves the forecast weights (alpha, beta, gamma) used for the system's forecasting
             * from the database. If the weights are not found, returns default weights.
             *
             * The method performs the following steps:
             * - Executes an SQL query to fetch the forecast weights from the 'forecast_weights' table.
             * - If a valid entry is found, the weights are retrieved and returned as a ForecastWeights object.
             * - If no entry is found, default values of 1.0 for alpha, beta, and gamma are returned.
             *
             * @return A ForecastWeights object containing the retrieved or default forecast weights.
             * @throws SQLException If a database access error occurs during the retrieval of forecast weights.
             */
            @Override
            public ForecastWeights getForecastWeights() throws SQLException {
                String sql = "SELECT alpha, beta, gamma FROM forecast_weights WHERE id=1";
                try (Connection connection = basicDataSource.getConnection();
                     Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        double alpha = rs.getDouble("alpha");
                        double beta = rs.getDouble("beta");
                        double gamma = rs.getDouble("gamma");
                        return new ForecastWeights(alpha, beta, gamma);
                    } else {
                        // Falls kein Eintrag existiert, Standardwerte zurückgeben oder anlegen
                        return new ForecastWeights(1.0, 1.0, 1.0);
                    }
                }
            }


            /**
             * Updates the forecast weights in the database with the provided alpha, beta, and gamma values.
             * If no record exists, a new record is created with the specified values.
             *
             * @param alpha The smoothing factor for the level in the forecast model.
             * @param beta  The smoothing factor for the trend in the forecast model.
             * @param gamma The smoothing factor for the seasonality in the forecast model.
             * @throws SQLException If a database access error occurs during the update process.
             */
            @Override
            public void updateForecastWeights(double alpha, double beta, double gamma) throws SQLException {
                String sql = "UPDATE forecast_weights SET alpha=?, beta=?, gamma=? WHERE id=1";
                try (Connection connection = basicDataSource.getConnection();
                     PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setDouble(1, alpha);
                    pstmt.setDouble(2, beta);
                    pstmt.setDouble(3, gamma);
                    int updated = pstmt.executeUpdate();
                    if (updated == 0) {
                        // Falls noch kein Datensatz da ist, lege ihn an
                        String insertSQL = "INSERT INTO forecast_weights (id, alpha, beta, gamma) VALUES (1, ?, ?, ?)";
                        try (PreparedStatement ins = connection.prepareStatement(insertSQL)) {
                            ins.setDouble(1, alpha);
                            ins.setDouble(2, beta);
                            ins.setDouble(3, gamma);
                            ins.executeUpdate();
                        }
                    }
                }
            }

            /**
             * Creates the 'forecast_weights' table in the database if it does not already exist.
             * The table is designed to store alpha, beta, and gamma parameters
             * used for forecasting calculations. These parameters are initialized
             * with default values of 1.0.
             *
             * The method also ensures that a default entry with `id=1` and all
             * parameters set to their default values is inserted into the table,
             * unless such an entry already exists.
             *
             * This operation involves creating the table structure and inserting
             * a default record, handled within a database connection. If any errors
             * occur during table creation or record insertion, a SQLException will be thrown.
             *
             * @throws SQLException if a database access error occurs, such as a connection failure
             *                      or an error executing the SQL statements.
             */
            @Override
            public void createForecastWeightsTable() throws SQLException {
                String createTableSQL = """
            CREATE TABLE IF NOT EXISTS forecast_weights (
                id SERIAL PRIMARY KEY,
                alpha DOUBLE PRECISION NOT NULL DEFAULT 1.0,
                beta  DOUBLE PRECISION NOT NULL DEFAULT 1.0,
                gamma DOUBLE PRECISION NOT NULL DEFAULT 1.0
            );
        """;

                try (Connection connection = basicDataSource.getConnection();
                     Statement stmt = connection.createStatement()) {
                    stmt.execute(createTableSQL);

                    // Optional: gleich einen Default-Datensatz (id=1) anlegen, falls nicht vorhanden
                    String insertDefaultSQL = """
                INSERT INTO forecast_weights (id, alpha, beta, gamma)
                VALUES (1, 1.0, 1.0, 1.0)
                ON CONFLICT DO NOTHING;
            """;
                    stmt.execute(insertDefaultSQL);
                }
            }


            /**
             * Partially updates the specified product in the database based on the provided fields.
             * Only fields present in the updates map will be updated, and any missing values will remain unchanged.
             *
             * @param productId The ID of the product to be updated.
             * @param updates A map containing the fields to be updated and their new values.
             *                Accepted keys are:
             *                - "dailyDemand": The new daily demand for the product.
             *                - "reorderPoint": The new reorder point for the product.
             *                - "reorderQuantity": The new reorder quantity for the product.
             *                - "productQuantity": The new quantity of the product in stock.
             *                - "producttype": The new type/category of the product.
             * @throws SQLException If a database access error occurs or the update fails.
             */
            @Override
            public void updateProductPartial(int productId, Map<String, Object> updates) throws SQLException {
                // Erstelle dynamisch das SQL-Update-Statement basierend auf den übergebenen Feldern
                StringBuilder sql = new StringBuilder("UPDATE products SET ");
                List<Object> params = new ArrayList<>();

                // Prüfe jedes mögliche Feld in der Map und füge es zur SQL-Abfrage hinzu, falls vorhanden
                if (updates.containsKey("dailyDemand")) {
                    sql.append("daily_demand = ?, ");
                    params.add(updates.get("dailyDemand"));
                }
                if (updates.containsKey("reorderPoint")) {
                    sql.append("reorder_point = ?, ");
                    params.add(updates.get("reorderPoint"));
                }
                if (updates.containsKey("reorderQuantity")) {
                    sql.append("reorder_quantity = ?, ");
                    params.add(updates.get("reorderQuantity"));
                }
                if (updates.containsKey("productQuantity")) {
                    sql.append("quantity = ?, ");
                    params.add(updates.get("productQuantity"));
                }
                if (updates.containsKey("producttype")) {
                    sql.append("producttype = ?, ");
                    params.add(updates.get("producttype"));
                }

                // Entferne das letzte Komma und Leerzeichen
                if (params.isEmpty()) {
                    // Falls keine Aktualisierungen übergeben wurden, beenden
                    return;
                }
                sql.setLength(sql.length() - 2);

                sql.append(" WHERE productid = ?");
                params.add(productId);

                try (Connection connection = basicDataSource.getConnection();
                     PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                    // Setze Parameter in der PreparedStatement
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Updating product failed, no rows affected.");
                    }
                }
            }

            /**
             * Retrieves the total quantity of all products available in the warehouse.
             *
             * This method executes a SQL query to calculate the sum of the "quantity" column
             * across all rows in the "products" table. If the table is empty, the result will
             * default to 0. The connection to the database is managed using the class's
             * configured data source, and resources are automatically closed after use.
             *
             * @return The total quantity of all products in the warehouse as an integer.
             *         Returns 0 if there are no entries in the "products" table or if an
             *         error occurs during the query execution.
             * @throws SQLException If an error occurs while accessing the database.
             */
            @Override
            public int getTotalWarehouseQuantity() throws SQLException {
                try (Connection conn = getDataSource().getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(quantity), 0) AS total FROM products")) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
                return 0;
            }

        }

