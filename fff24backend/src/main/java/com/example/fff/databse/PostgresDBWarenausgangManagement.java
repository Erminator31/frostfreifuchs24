package com.example.fff.databse;

import com.example.fff.api.WarenausgangManager;
import com.example.fff.model.Warenausgang;
import com.example.fff.model.WarenausgangItem;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PostgresDBWarenausgangManagement implements WarenausgangManager {

    String databaseURL = "jdbc:postgresql://c7u1tn6bvvsodf.cluster-czz5s0kz4scl.eu-west-1.rds.amazonaws.com:5432/d1t207hd56v54?sslmode=require";
    String username = "u3t73itv4ifknl";
    String password = "pc8d79bc3deea2ca2b99f04d14057aeb257bac911861af2c1c3f890ffecaa803c";

    BasicDataSource basicDataSource;

    private static PostgresDBWarenausgangManagement instance = null;

    private static final Logger LOGGER = Logger.getLogger(PostgresDBWarenausgangManagement.class.getName());

    private PostgresDBWarenausgangManagement() {
        basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(databaseURL);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
    }

    public static PostgresDBWarenausgangManagement getInstance() {
        if (instance == null) {
            instance = new PostgresDBWarenausgangManagement();
        }
        return instance;
    }

    @Override
    public void createWarenausgangTable() throws Exception {
        Connection connection = null;
        PreparedStatement pstmt = null;
        String createTableSQL = "CREATE TABLE IF NOT EXISTS warenausgaenge ("
                + "warenausgangid SERIAL PRIMARY KEY, "
                + "warenausgangdate TIMESTAMP NOT NULL DEFAULT NOW(), "
                + "quantity INT DEFAULT NULL "
                + ");";

        try {
            connection = basicDataSource.getConnection();
            pstmt = connection.prepareStatement(createTableSQL);
            pstmt.execute();
        } catch (SQLException e) {
            throw new Exception("Error creating warenausgaenge table", e);
        } finally {
            if (pstmt != null)
                pstmt.close();
            if (connection != null)
                connection.close();
        }
    }

    @Override
    public void createWarenausgangItemTable() throws Exception {
        Connection connection = null;
        PreparedStatement pstmt = null;
        String createTableSQL = "CREATE TABLE IF NOT EXISTS warenausgang_items ("
                + "warenausgangitemid SERIAL PRIMARY KEY, "
                + "warenausgangid INT NOT NULL, "
                + "productid INT NOT NULL, "
                + "quantity INT DEFAULT NULL, "
                + "FOREIGN KEY (warenausgangid) REFERENCES warenausgaenge(warenausgangid) ON DELETE CASCADE, "
                + "FOREIGN KEY (productid) REFERENCES products(productid) ON DELETE CASCADE "
                + ");";

        try {
            connection = basicDataSource.getConnection();
            pstmt = connection.prepareStatement(createTableSQL);
            pstmt.execute();
        } catch (SQLException e) {
            throw new Exception("Error creating warenausgang_items table", e);
        } finally {
            if (pstmt != null)
                pstmt.close();
            if (connection != null)
                connection.close();
        }
    }

    @Override
    public Warenausgang createWarenausgang(List<WarenausgangItem> items) throws Exception {
        return createWarenausgang(items, new Timestamp(System.currentTimeMillis()));
    }

    @Override
    public Warenausgang createWarenausgang(List<WarenausgangItem> items, Timestamp warenausgangDate) throws Exception {
        Connection connection = null;
        PreparedStatement warenausgangStmt = null;
        PreparedStatement warenausgangItemStmt = null;
        PreparedStatement updateProductStmt = null;
        PreparedStatement checkDemandStmt = null;
        PreparedStatement updateReorderStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();
            connection.setAutoCommit(false);

            // Warenausgang anlegen
            String insertWarenausgangSQL = "INSERT INTO warenausgaenge (warenausgangdate) VALUES (?) RETURNING warenausgangid, warenausgangdate";
            warenausgangStmt = connection.prepareStatement(insertWarenausgangSQL);
            warenausgangStmt.setTimestamp(1, warenausgangDate);
            rs = warenausgangStmt.executeQuery();

            int newWarenausgangId = -1;
            Timestamp returnedDate = null;
            if (rs.next()) {
                newWarenausgangId = rs.getInt("warenausgangid");
                returnedDate = rs.getTimestamp("warenausgangdate");
            }

            if (newWarenausgangId == -1) {
                throw new SQLException("Could not create warenausgang");
            }

            String selectProductSQL = "SELECT quantity, daily_demand, reorder_point, reorder_quantity FROM products WHERE productid = ? FOR UPDATE";
            String updateProductSQL = "UPDATE products SET quantity = quantity - ?, daily_demand = ?, reorder_point = ? WHERE productid = ?";
            String insertWarenausgangItemSQL = "INSERT INTO warenausgang_items (warenausgangid, productid, quantity) VALUES (?, ?, ?)";

            warenausgangItemStmt = connection.prepareStatement(insertWarenausgangItemSQL);
            updateProductStmt = connection.prepareStatement(updateProductSQL);

            for (WarenausgangItem item : items) {
                // Produktbestand prüfen
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

                // Warenausgang Item einfügen
                warenausgangItemStmt.setInt(1, newWarenausgangId);
                warenausgangItemStmt.setInt(2, item.getProductId());
                warenausgangItemStmt.setInt(3, item.getQuantity());
                warenausgangItemStmt.addBatch();

                // Bestand reduzieren
                updateProductStmt.setInt(1, item.getQuantity());
                updateProductStmt.setInt(2, dailyDemand); // Kann später an Bedarf angepasst werden
                updateProductStmt.setInt(3, reorderPoint);
                updateProductStmt.setInt(4, item.getProductId());
                updateProductStmt.addBatch();
            }

            warenausgangItemStmt.executeBatch();
            updateProductStmt.executeBatch();

            // Durchschnittlichen Bedarf berechnen und Reorder Point aktualisieren
            for (WarenausgangItem item : items) {
                double averageDailyDemand = calculateAverageDailyDemand(item.getProductId(), connection);

                String updateReorderSQL = "UPDATE products SET daily_demand = ?, reorder_point = ? WHERE productid = ?";
                updateReorderStmt = connection.prepareStatement(updateReorderSQL);
                updateReorderStmt.setDouble(1, averageDailyDemand);
                updateReorderStmt.setDouble(2, averageDailyDemand * 7);
                updateReorderStmt.setInt(3, item.getProductId());
                updateReorderStmt.executeUpdate();
                updateReorderStmt.close();

                // Überprüfen, ob Bestand unter den Reorder Point gefallen ist
                String checkReorderSQL = "SELECT quantity, reorder_quantity FROM products WHERE productid = ?";
                PreparedStatement psCheck = connection.prepareStatement(checkReorderSQL);
                psCheck.setInt(1, item.getProductId());
                ResultSet reorderRs = psCheck.executeQuery();

                if (reorderRs.next()) {
                    int currentQty = reorderRs.getInt("quantity");
                    int reorderQty = reorderRs.getInt("reorder_quantity");

                    if (currentQty < averageDailyDemand * 7) {
                        // Nachbestellen (Wareneingang verbuchen)
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

            return new Warenausgang(newWarenausgangId, returnedDate.toString(), items);

        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
            }
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (warenausgangStmt != null) warenausgangStmt.close();
            if (warenausgangItemStmt != null) warenausgangItemStmt.close();
            if (updateProductStmt != null) updateProductStmt.close();
            if (checkDemandStmt != null) checkDemandStmt.close();
            if (updateReorderStmt != null) updateReorderStmt.close();
            if (connection != null) connection.close();
        }
    }

    @Override
    public Warenausgang getWarenausgang(int warenausgangId) throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        PreparedStatement itemStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();

            String selectWarenausgangSQL = "SELECT warenausgangid, warenausgangdate FROM warenausgaenge WHERE warenausgangid = ?";
            stmt = connection.prepareStatement(selectWarenausgangSQL);
            stmt.setInt(1, warenausgangId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            int wId = rs.getInt("warenausgangid");
            String warenausgangDate = rs.getString("warenausgangdate");

            // Items holen
            String selectItemsSQL = "SELECT productid, quantity FROM warenausgang_items WHERE warenausgangid = ?";
            itemStmt = connection.prepareStatement(selectItemsSQL);
            itemStmt.setInt(1, wId);
            try (ResultSet itemRS = itemStmt.executeQuery()) {
                List<WarenausgangItem> items = new ArrayList<>();
                while (itemRS.next()) {
                    items.add(new WarenausgangItem(itemRS.getInt("productid"), itemRS.getInt("quantity")));
                }
                return new Warenausgang(wId, warenausgangDate, items);
            }

        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (itemStmt != null) itemStmt.close();
            if (connection != null) connection.close();
        }
    }

    @Override
    public List<Warenausgang> getAllWarenausgaenge() throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Warenausgang> warenausgaenge = new ArrayList<>();

        try {
            connection = basicDataSource.getConnection();
            String selectSQL = "SELECT warenausgangid, warenausgangdate FROM warenausgaenge";
            stmt = connection.prepareStatement(selectSQL);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int warenausgangId = rs.getInt("warenausgangid");
                // Items holen über getWarenausgang()
                Warenausgang w = getWarenausgang(warenausgangId);
                if (w != null) {
                    warenausgaenge.add(w);
                }
            }

            return warenausgaenge;

        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    @Override
    public void deleteWarenausgangTable() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        String dropTableSQL = "DROP TABLE IF EXISTS warenausgaenge CASCADE;";

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
    public void deleteWarenausgangItemsTable() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        String dropTableSQL = "DROP TABLE IF EXISTS warenausgang_items CASCADE;";

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
        String query = "SELECT o.warenausgangdate, oi.quantity FROM warenausgaenge o "
                + "JOIN warenausgang_items oi ON o.warenausgangid = oi.warenausgangid "
                + "WHERE oi.productid = ? "
                + "ORDER BY o.warenausgangdate DESC "
                + "LIMIT 10;";

        List<Timestamp> ausgangDates = new ArrayList<>();
        int totalQuantity = 0;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp date = rs.getTimestamp("warenausgangdate");
                    int quantity = rs.getInt("quantity");
                    ausgangDates.add(date);
                    totalQuantity += quantity;
                }
            }
        }

        if (ausgangDates.isEmpty()) {
            return 0.0;
        }

        // Zeitraum zwischen ältester und neuester Warenausgang
        Timestamp oldest = ausgangDates.get(ausgangDates.size() - 1);
        Timestamp newest = ausgangDates.get(0);
        long milliseconds = newest.getTime() - oldest.getTime();
        double days = milliseconds / (1000.0 * 60 * 60 * 24);

        if (days == 0) {
            days = 1;
        }

        return totalQuantity / days;
    }
}
