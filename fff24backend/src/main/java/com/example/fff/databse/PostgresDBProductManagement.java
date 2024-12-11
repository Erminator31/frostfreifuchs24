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
                + "quantity INT DEFAULT NULL "
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
     * Adds a new product with the given product name and product type to the database.
     *
     * @param productName The name of the product to be added.
     * @param productType The type of the product to be added.
     * @return The newly created Product object if the addition was successful, null otherwise.
     */
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
                            rs.getString("producttype")
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
}
