package com.example.fff.databse;

import com.example.fff.api.ProductManager;
import com.example.fff.model.Product;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostgresDBProductManagement implements ProductManager {
    // Database connection details
// Database connection details
    String databaseURL = "jdbc:postgresql://c7u1tn6bvvsodf.cluster-czz5s0kz4scl.eu-west-1.rds.amazonaws.com:5432/d1t207hd56v54?sslmode=require";
    String username = "u3t73itv4ifknl";
    String password = "pc8d79bc3deea2ca2b99f04d14057aeb257bac911861af2c1c3f890ffecaa803c";


    BasicDataSource basicDataSource;

    // Singleton pattern for the manager implementation
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
     * @param rs The ResultSet to be closed.
     * @param stmt The PreparedStatement to be closed.
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


    @Override
    public Product addProduct(String productName, String productType, int quantity) throws Exception {
        // Initialer daily_demand ist 1000
        int initialDailyDemand = 35;
        return addProduct(productName, productType, quantity, initialDailyDemand, initialDailyDemand * 3); // reorderPoint = dailyDemand * 3
    }

    /**
     * Erweiterte Methode zum Hinzufügen eines Produkts mit spezifischen Reorder-Parametern.
     *
     * @param productName     Der Name des Produkts.
     * @param productType     Der Typ des Produkts.
     * @param quantity        Die Menge, die hinzugefügt werden soll.
     * @param dailyDemand     Der tägliche Bedarf.
     * @param reorderPoint    Der Reorder Point (dailyDemand * 3).
     * @return Das erstellte oder aktualisierte Produkt.
     * @throws Exception Wenn ein Fehler auftritt.
     */
    public Product addProduct(String productName, String productType, int quantity, int dailyDemand, int reorderPoint) throws Exception {
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

            // Aktuelle Gesamtmenge im Lager prüfen (Maximal 20000)
            String sumSQL = "SELECT COALESCE(SUM(quantity), 0) AS total_quantity FROM products";
            sumStmt = connection.prepareStatement(sumSQL);
            rs = sumStmt.executeQuery();
            int currentTotalQuantity = 0;
            if (rs.next()) {
                currentTotalQuantity = rs.getInt("total_quantity");
            }
            rs.close();
            sumStmt.close();

            if (currentTotalQuantity + quantity > 200000) {
                connection.rollback();
                throw new Exception("Cannot add product. Adding " + quantity + " units would exceed the total warehouse capacity of 20000.");
            }

            // Prüfen, ob das Produkt bereits existiert
            String checkProductSQL = "SELECT productid, quantity, daily_demand, reorder_point FROM products WHERE productname = ? AND producttype = ?";
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
                int newReorderPoint = newDailyDemand * 3;
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
                return new Product(productId, productName, productType, newQuantity, newDailyDemand, newReorderPoint);

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
                insertStmt.setInt(6, dailyDemand * 14); // reorderQuantity = dailyDemand * 14

                rs = insertStmt.executeQuery();
                int generatedId = -1;
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                } else {
                    connection.rollback();
                    throw new SQLException("Creating product failed, no ID obtained.");
                }

                connection.commit();
                return new Product(generatedId, productName, productType, quantity, dailyDemand, reorderPoint);
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
                            rs.getInt("reorder_point")

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
                try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (connection != null) {
                try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }
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

            PostgresDBOrderManagement orderManager = PostgresDBOrderManagement.getInstance();

            for (int productId : productIds) {
                double avgDailyDemand = orderManager.calculateAverageDailyDemand(productId, connection);
                int newDailyDemand = (int) Math.round(avgDailyDemand);
                int newReorderPoint = newDailyDemand * 3;
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

}
