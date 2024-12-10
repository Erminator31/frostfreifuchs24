package com.example.fff.databse;

import com.example.fff.api.ProductManager;
import model.Product;
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
     * Private constructor initializes the data source.
     */
    private PostgresDBProductManagement() {
        basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(databaseURL);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
    }
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
     * Singleton pattern: Get an instance of this manager.
     * Create it if it doesn't exist.
     *
     * @return instance of PostgresDBEventManagerImpl
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
                + "producttype VARCHAR(100) NOT NULL"
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
    public Product addProduct(String productName, String productType) {

        final Logger createProductLogger = Logger.getLogger("CreateProductLogger");
        createProductLogger.log(Level.INFO, "Start creating product: " + productName);

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Verbindung holen (basicDataSource sollte bereits konfiguriert sein)
            connection = basicDataSource.getConnection();

            // INSERT-Statement für das Einfügen eines neuen Produkts
            // Wir setzen RETURN_GENERATED_KEYS, um den automatisch generierten productId-Wert abzufangen
            String insertSQL = "INSERT INTO products (productName, productType) VALUES (?, ?)";
            stmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, productName);
            stmt.setString(2, productType);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }

            // Der neu generierte Schlüssel (productId) wird hier ausgelesen
            rs = stmt.getGeneratedKeys();
            int generatedId = -1;
            if (rs.next()) {
                generatedId = rs.getInt(1);
            } else {
                throw new SQLException("Creating product failed, no ID obtained.");
            }

            // Neues Produktobjekt mit generierter ID zurückgeben
            return new Product(generatedId, productName, productType);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Ressourcen im finally-Block schließen
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (connection != null) {
                try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }

        // Falls ein Fehler auftritt, geben wir null zurück oder werfen eine RuntimeException
        return null;
    }
    @Override
    public List<Product> readAllProducts() {
        final Logger readProductLogger = Logger.getLogger("ReadProductLogger");
        readProductLogger.log(Level.INFO, "Start reading products");

        List<Product> products = new ArrayList<>();
        final String query = "SELECT * FROM products";

        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("productid"),
                        rs.getString("productname"),
                        rs.getString("producttype")
                ));
            }
        } catch (SQLException e) {
            readProductLogger.log(Level.SEVERE, "Error reading products", e);
        }

        return products;
    }



}
