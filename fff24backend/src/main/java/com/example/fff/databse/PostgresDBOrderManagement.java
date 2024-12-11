package com.example.fff.databse;

import com.example.fff.api.OrderManager;


import model.Order;
import model.OrderItem;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PostgresDBOrderManagement implements OrderManager {

    String databaseURL = "jdbc:postgresql://c7u1tn6bvvsodf.cluster-czz5s0kz4scl.eu-west-1.rds.amazonaws.com:5432/d1t207hd56v54?sslmode=require";
    String username = "u3t73itv4ifknl";
    String password = "pc8d79bc3deea2ca2b99f04d14057aeb257bac911861af2c1c3f890ffecaa803c";

    BasicDataSource basicDataSource;

    private static PostgresDBOrderManagement postgresDBOrderManagement = null;

    private static final Logger LOGGER = Logger.getLogger(PostgresDBOrderManagement.class.getName());

    private PostgresDBOrderManagement() {
        basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(databaseURL);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
    }

    public static PostgresDBOrderManagement getInstance() {
        if (postgresDBOrderManagement == null) {
            postgresDBOrderManagement = new PostgresDBOrderManagement();
        }
        return postgresDBOrderManagement;
    }

    @Override
    public void createOrderTable() throws Exception {
        Connection connection = null;
        PreparedStatement pstmt = null;
        // CREATE TABLE Statement korrigiert und auf products angepasst
        String createTableSQL = "CREATE TABLE IF NOT EXISTS orders ("
                + "orderid SERIAL PRIMARY KEY, "
                + "orderdate TIMESTAMP NOT NULL DEFAULT NOW(), "
                + "customername VARCHAR(255), "
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

    @Override
    public void createOrderItemTable() throws Exception {
        Connection connection = null;
        PreparedStatement pstmt = null;
        // CREATE TABLE Statement korrigiert und auf products angepasst
        String createTableSQL = "CREATE TABLE IF NOT EXISTS order_items ("
                + "orderitemid SERIAL PRIMARY KEY, "
                + "orderid INT NOT NULL, "
                + "productid INT NOT NULL, "
                + "quantity INT DEFAULT NULL, "
       + "FOREIGN KEY (orderid) REFERENCES orders(orderid) ON DELETE CASCADE, "
                + "FOREIGN KEY (productid) REFERENCES products(productid) ON DELETE CASCADE "
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
    public Order createOrder(String customerName, List<OrderItem> items) throws Exception {
        Connection connection = null;
        PreparedStatement orderStmt = null;
        PreparedStatement orderItemStmt = null;
        PreparedStatement updateProductStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();
            connection.setAutoCommit(false);

            // 1. Neuen Order-Eintrag erzeugen
            String insertOrderSQL = "INSERT INTO orders (customername) VALUES (?) RETURNING orderid, orderdate";
            orderStmt = connection.prepareStatement(insertOrderSQL);
            orderStmt.setString(1, customerName);
            rs = orderStmt.executeQuery();

            int newOrderId = -1;
            String orderDate = null;
            if (rs.next()) {
                newOrderId = rs.getInt("orderid");
                orderDate = rs.getString("orderdate");
            }

            if (newOrderId == -1) {
                throw new SQLException("Could not create order");
            }

            // 2. Für jedes OrderItem prüfen, ob genügend Bestand da ist
            // Dazu holen wir uns jeweils den aktuellen Bestand aus der DB
            String selectProductSQL = "SELECT quantity FROM products WHERE productid = ? FOR UPDATE";
            // Bestände aktualisieren
            String updateProductSQL = "UPDATE products SET quantity = quantity - ? WHERE productid = ?";

            // Order Items einfügen
            String insertOrderItemSQL = "INSERT INTO order_items (orderid, productid, quantity) VALUES (?, ?, ?)";

            orderItemStmt = connection.prepareStatement(insertOrderItemSQL);
            updateProductStmt = connection.prepareStatement(updateProductSQL);

            PostgresDBProductManagement productManager = PostgresDBProductManagement.getPostgresDBProductManagement();

            for (OrderItem item : items) {
                // Produktbestand checken
                int currentStock = getCurrentStock(connection, item.getProductId());
                if (item.getQuantity() > currentStock) {
                    throw new Exception("Not enough stock for productId: " + item.getProductId());
                }

                // Order Item einfügen
                orderItemStmt.setInt(1, newOrderId);
                orderItemStmt.setInt(2, item.getProductId());
                orderItemStmt.setInt(3, item.getQuantity());
                orderItemStmt.addBatch();

                // Bestand reduzieren
                updateProductStmt.setInt(1, item.getQuantity());
                updateProductStmt.setInt(2, item.getProductId());
                updateProductStmt.addBatch();
            }

            orderItemStmt.executeBatch();
            updateProductStmt.executeBatch();

            connection.commit();

            // Erfolgreich --> Order als Objekt zurückgeben
            return new Order(newOrderId, customerName, orderDate, items);

        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
            }
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (orderStmt != null) orderStmt.close();
            if (orderItemStmt != null) orderItemStmt.close();
            if (updateProductStmt != null) updateProductStmt.close();
            if (connection != null) connection.close();
        }
    }

    @Override
    public Order getOrder(int orderId) throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        PreparedStatement itemStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();

            String selectOrderSQL = "SELECT orderid, customername, orderdate FROM orders WHERE orderid = ?";
            stmt = connection.prepareStatement(selectOrderSQL);
            stmt.setInt(1, orderId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            int oId = rs.getInt("orderid");
            String customerName = rs.getString("customername");
            String orderDate = rs.getString("orderdate");

            // Items holen
            String selectItemsSQL = "SELECT productid, quantity FROM order_items WHERE orderid = ?";
            itemStmt = connection.prepareStatement(selectItemsSQL);
            itemStmt.setInt(1, oId);
            try (ResultSet itemRS = itemStmt.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (itemRS.next()) {
                    items.add(new OrderItem(itemRS.getInt("productid"), itemRS.getInt("quantity")));
                }
                return new Order(oId, customerName, orderDate, items);
            }

        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (itemStmt != null) itemStmt.close();
            if (connection != null) connection.close();
        }
    }

    @Override
    public List<Order> getAllOrders() throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Order> orders = new ArrayList<>();

        try {
            connection = basicDataSource.getConnection();
            String selectSQL = "SELECT orderid, customername, orderdate FROM orders";
            stmt = connection.prepareStatement(selectSQL);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int orderId = rs.getInt("orderid");
                String customerName = rs.getString("customername");
                String orderDate = rs.getString("orderdate");

                // Items holen
                orders.add(getOrder(orderId));
            }

            return orders;

        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    private int getCurrentStock(Connection connection, int productId) throws SQLException {
        String sql = "SELECT quantity FROM products WHERE productid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                } else {
                    throw new SQLException("Product not found: " + productId);
                }
            }
        }
    }
    @Override
    public void deleteOrderTable() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        String dropTableSQL = "DROP TABLE IF EXISTS orders CASCADE;";

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
    @Override
    public void deleteOrderItemsTable() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        String dropTableSQL = "DROP TABLE IF EXISTS order_itmes CASCADE;";

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

