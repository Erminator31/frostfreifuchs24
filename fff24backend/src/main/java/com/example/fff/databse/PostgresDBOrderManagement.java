package com.example.fff.databse;

import com.example.fff.api.OrderManager;


import com.example.fff.model.Order;
import com.example.fff.model.OrderItem;
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


    /**
     * Erstellt eine neue Bestellung.
     *
     * @param customerName Der Name des Kunden.
     * @param items        Die bestellten Artikel.
     * @return Die erstellte Bestellung.
     * @throws Exception Wenn ein Fehler auftritt.
     */
    @Override
    public Order createOrder(String customerName, List<OrderItem> items) throws Exception {
        // Standardmethode ohne Datum
        return createOrder(customerName, items, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Erstellt eine neue Bestellung mit einem spezifischen Datum.
     *
     * @param customerName Der Name des Kunden.
     * @param items        Die bestellten Artikel.
     * @param orderDate    Das Datum der Bestellung.
     * @return Die erstellte Bestellung.
     * @throws Exception Wenn ein Fehler auftritt.
     */
    @Override
    public Order createOrder(String customerName, List<OrderItem> items, Timestamp orderDate) throws Exception {
        Connection connection = null;
        PreparedStatement orderStmt = null;
        PreparedStatement orderItemStmt = null;
        PreparedStatement updateProductStmt = null;
        PreparedStatement checkDemandStmt = null;
        PreparedStatement updateReorderStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();
            connection.setAutoCommit(false);

            // 1. Neuen Order-Eintrag erzeugen
            String insertOrderSQL = "INSERT INTO orders (orderdate, customername) VALUES (?, ?) RETURNING orderid, orderdate";
            orderStmt = connection.prepareStatement(insertOrderSQL);
            orderStmt.setTimestamp(1, orderDate);
            orderStmt.setString(2, customerName);
            rs = orderStmt.executeQuery();

            int newOrderId = -1;
            Timestamp returnedOrderDate = null;
            if (rs.next()) {
                newOrderId = rs.getInt("orderid");
                returnedOrderDate = rs.getTimestamp("orderdate");
            }

            if (newOrderId == -1) {
                throw new SQLException("Could not create order");
            }

            // 2. Für jedes OrderItem prüfen, ob genügend Bestand da ist und aktualisieren
            String selectProductSQL = "SELECT quantity, daily_demand, reorder_point, reorder_quantity FROM products WHERE productid = ? FOR UPDATE";
            String updateProductSQL = "UPDATE products SET quantity = quantity - ?, daily_demand = ?, reorder_point = ? WHERE productid = ?";
            String insertOrderItemSQL = "INSERT INTO order_items (orderid, productid, quantity) VALUES (?, ?, ?)";

            orderItemStmt = connection.prepareStatement(insertOrderItemSQL);
            updateProductStmt = connection.prepareStatement(updateProductSQL);

            for (OrderItem item : items) {
                // Produktbestand und täglicher Bedarf abrufen
                PreparedStatement ps = connection.prepareStatement(selectProductSQL);
                ps.setInt(1, item.getProductId());
                ResultSet productRs = ps.executeQuery();

                if (!productRs.next()) {
                    throw new Exception("Product not found: " + item.getProductId());
                }

                int currentStock = productRs.getInt("quantity");
                int dailyDemand = productRs.getInt("daily_demand");
                int reorderPoint = productRs.getInt("reorder_point");
                int reorderQuantity = productRs.getInt("reorder_quantity");
                productRs.close();
                ps.close();

                if (item.getQuantity() > currentStock) {
                    throw new Exception("Not enough stock for productId: " + item.getProductId());
                }

                // Bestellartikel einfügen
                orderItemStmt.setInt(1, newOrderId);
                orderItemStmt.setInt(2, item.getProductId());
                orderItemStmt.setInt(3, item.getQuantity());
                orderItemStmt.addBatch();

                // Bestand reduzieren und daily_demand aktualisieren
                updateProductStmt.setInt(1, item.getQuantity());
                updateProductStmt.setInt(2, dailyDemand); // Hier können Sie später die Aktualisierung basierend auf dem Bedarf hinzufügen
                updateProductStmt.setInt(3, reorderPoint); // Kann ebenfalls angepasst werden
                updateProductStmt.setInt(4, item.getProductId());
                updateProductStmt.addBatch();
            }

            orderItemStmt.executeBatch();
            updateProductStmt.executeBatch();

            // 3. Berechnung des durchschnittlichen täglichen Bedarfs der letzten 10 Tage und Aktualisierung des Reorder Points
            for (OrderItem item : items) {
                // Durchschnittlichen täglichen Bedarf der letzten 10 Tage berechnen
                String calculateDemandSQL = "SELECT COUNT(*) AS orders_count FROM order_items oi "
                        + "JOIN orders o ON oi.orderid = o.orderid "
                        + "WHERE oi.productid = ? AND o.orderdate >= ?";
                checkDemandStmt = connection.prepareStatement(calculateDemandSQL);
                checkDemandStmt.setInt(1, item.getProductId());

                // 10 Tage zurück ab dem aktuellen orderDate
                Timestamp tenDaysAgo = new Timestamp(orderDate.getTime() - (10L * 24 * 60 * 60 * 1000));
                checkDemandStmt.setTimestamp(2, tenDaysAgo);
                ResultSet demandRs = checkDemandStmt.executeQuery();

                int ordersCount = 0;
                if (demandRs.next()) {
                    ordersCount = demandRs.getInt("orders_count");
                }
                demandRs.close();
                checkDemandStmt.close();



// Durchschnittlicher täglicher Bedarf basierend auf den letzten 10 Bestellungen
                double averageDailyDemand = calculateAverageDailyDemand(item.getProductId(), connection);

// Aktualisieren des täglichen Bedarfs und des Reorder Points
                String updateReorderSQL = "UPDATE products SET daily_demand = ?, reorder_point = ? WHERE productid = ?";
                updateReorderStmt = connection.prepareStatement(updateReorderSQL);
                updateReorderStmt.setDouble(1, averageDailyDemand);
                updateReorderStmt.setDouble(2, averageDailyDemand * 3); // 3 Tage Lieferzeit
                updateReorderStmt.setInt(3, item.getProductId());
                updateReorderStmt.executeUpdate();

                updateReorderStmt.close();

                // 4. Überprüfen, ob der Bestand unter den Reorder Point gefallen ist
                String checkReorderSQL = "SELECT quantity, reorder_quantity FROM products WHERE productid = ?";
                PreparedStatement psCheck = connection.prepareStatement(checkReorderSQL);
                psCheck.setInt(1, item.getProductId());
                ResultSet reorderRs = psCheck.executeQuery();

                if (reorderRs.next()) {
                    int currentQty = reorderRs.getInt("quantity");
                    int reorderQty = reorderRs.getInt("reorder_quantity");

                    if (currentQty < averageDailyDemand * 3) {
                        // Nachbestellen
                        String restockSQL = "UPDATE products SET quantity = quantity + ? WHERE productid = ?";
                        PreparedStatement psRestock = connection.prepareStatement(restockSQL);
                        psRestock.setInt(1, reorderQty);
                        psRestock.setInt(2, item.getProductId());
                        psRestock.executeUpdate();
                        psRestock.close();
                    }
                }
                reorderRs.close();
                psCheck.close();
            }

            connection.commit();

            // Erfolgreich --> Order als Objekt zurückgeben
            return new Order(newOrderId, customerName, returnedOrderDate.toString(), items);

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
            if (checkDemandStmt != null) checkDemandStmt.close();
            if (updateReorderStmt != null) updateReorderStmt.close();
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
        String dropTableSQL = "DROP TABLE IF EXISTS order_items CASCADE;";

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
    public double calculateAverageDailyDemand(int productId, Connection connection) throws SQLException {
        String query = "SELECT o.orderdate, oi.quantity FROM orders o "
                + "JOIN order_items oi ON o.orderid = oi.orderid "
                + "WHERE oi.productid = ? "
                + "ORDER BY o.orderdate DESC "
                + "LIMIT 10;";

        List<Timestamp> orderDates = new ArrayList<>();
        int totalQuantity = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp orderDate = rs.getTimestamp("orderdate");
                    int quantity = rs.getInt("quantity");
                    orderDates.add(orderDate);
                    totalQuantity += quantity;
                }
            }
        }

        if (orderDates.isEmpty()) {
            return 0.0;
        }

        // Bestimmen Sie den Zeitraum zwischen der ältesten und der neuesten Bestellung
        Timestamp oldestOrder = orderDates.get(orderDates.size() - 1);
        Timestamp newestOrder = orderDates.get(0);
        long milliseconds = newestOrder.getTime() - oldestOrder.getTime();
        double days = milliseconds / (1000.0 * 60 * 60 * 24);

        // Vermeiden Sie Division durch Null
        if (days == 0) {
            days = 1;
        }

        // Durchschnittlicher täglicher Bedarf
        return totalQuantity / days;
    }




}

