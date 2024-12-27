package com.example.fff.databse;

import com.example.fff.api.WarenausgangManager;
import com.example.fff.api.WareneingangManager;
import com.example.fff.model.Warenausgang;
import com.example.fff.model.WarenausgangItem;
import com.example.fff.model.WareneingangItem;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Service
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
        try (Connection connection = basicDataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS warenausgaenge (" +
                             "warenausgangid SERIAL PRIMARY KEY, " +
                             "warenausgangdate TIMESTAMP NOT NULL DEFAULT NOW(), " +
                             "quantity INT DEFAULT NULL " +
                             ");")) {
            pstmt.execute();
        } catch (SQLException e) {
            throw new Exception("Error creating warenausgaenge table", e);
        }
    }

    @Override
    public void createWarenausgangItemTable() throws Exception {
        try (Connection connection = basicDataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS warenausgang_items (" +
                             "warenausgangitemid SERIAL PRIMARY KEY, " +
                             "warenausgangid INT NOT NULL, " +
                             "productid INT NOT NULL, " +
                             "quantity INT DEFAULT NULL, " +
                             "FOREIGN KEY (warenausgangid) REFERENCES warenausgaenge(warenausgangid) ON DELETE CASCADE, " +
                             "FOREIGN KEY (productid) REFERENCES products(productid) ON DELETE CASCADE " +
                             ");")) {
            pstmt.execute();
        } catch (SQLException e) {
            throw new Exception("Error creating warenausgang_items table", e);
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
        PreparedStatement updateReorderStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();
            connection.setAutoCommit(false);

            // Insert Warenausgang
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
                // Check product stock
                try (PreparedStatement ps = connection.prepareStatement(selectProductSQL)) {
                    ps.setInt(1, item.getProductId());
                    try (ResultSet productRs = ps.executeQuery()) {
                        if (!productRs.next()) {
                            throw new Exception("Product not found: " + item.getProductId());
                        }

                        int currentStock = productRs.getInt("quantity");
                        int dailyDemand = productRs.getInt("daily_demand");
                        int reorderPoint = productRs.getInt("reorder_point");
                        int reorderQuantity = productRs.getInt("reorder_quantity");

                        if (item.getQuantity() > currentStock) {
                            throw new Exception("Not enough stock for productId: " + item.getProductId());
                        }

                        // Insert Warenausgang Item
                        warenausgangItemStmt.setInt(1, newWarenausgangId);
                        warenausgangItemStmt.setInt(2, item.getProductId());
                        warenausgangItemStmt.setInt(3, item.getQuantity());
                        warenausgangItemStmt.addBatch();

                        // Reduce stock
                        updateProductStmt.setInt(1, item.getQuantity());
                        updateProductStmt.setInt(2, dailyDemand);
                        updateProductStmt.setInt(3, reorderPoint);
                        updateProductStmt.setInt(4, item.getProductId());
                        updateProductStmt.addBatch();
                    }
                }
            }

            warenausgangItemStmt.executeBatch();
            updateProductStmt.executeBatch();

            // Update daily demand and reorder point for each product
            for (WarenausgangItem item : items) {
                double averageDailyDemand = calculateAverageDailyDemand(item.getProductId(), connection);

                String updateReorderSQL = "UPDATE products SET daily_demand = ?, reorder_point = ? WHERE productid = ?";
                updateReorderStmt = connection.prepareStatement(updateReorderSQL);
                updateReorderStmt.setDouble(1, averageDailyDemand);
                updateReorderStmt.setDouble(2, averageDailyDemand * 7);
                updateReorderStmt.setInt(3, item.getProductId());
                updateReorderStmt.executeUpdate();
                updateReorderStmt.close();

                // Check if we need to restock (if currentQty < averageDailyDemand * 7)
                String checkReorderSQL = "SELECT quantity, reorder_quantity FROM products WHERE productid = ?";
                try (PreparedStatement psCheck = connection.prepareStatement(checkReorderSQL)) {
                    psCheck.setInt(1, item.getProductId());
                    try (ResultSet reorderRs = psCheck.executeQuery()) {
                        if (reorderRs.next()) {
                            int currentQty = reorderRs.getInt("quantity");
                            int reorderQty = reorderRs.getInt("reorder_quantity");

                            if (currentQty < averageDailyDemand * 7) {
                                // Trigger a wareneingang using WareneingangManager
                                WareneingangManager wareneingangManager = PostgresDBWareneingangManagement.getInstance();
                                List<WareneingangItem> eingangItems = Arrays.asList(new WareneingangItem(item.getProductId(), reorderQty));
                                wareneingangManager.createWareneingang(eingangItems);
                            }
                        }
                    }
                }
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
            if (updateReorderStmt != null) updateReorderStmt.close();
            if (connection != null) connection.close();
        }
    }

    @Override
    public Warenausgang getWarenausgang(int warenausgangId) throws Exception {
        try (Connection connection = basicDataSource.getConnection()) {
            String selectWarenausgangSQL = "SELECT warenausgangid, warenausgangdate FROM warenausgaenge WHERE warenausgangid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(selectWarenausgangSQL)) {
                stmt.setInt(1, warenausgangId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }

                    int wId = rs.getInt("warenausgangid");
                    String warenausgangDate = rs.getString("warenausgangdate");

                    String selectItemsSQL = "SELECT productid, quantity FROM warenausgang_items WHERE warenausgangid = ?";
                    try (PreparedStatement itemStmt = connection.prepareStatement(selectItemsSQL)) {
                        itemStmt.setInt(1, wId);
                        try (ResultSet itemRS = itemStmt.executeQuery()) {
                            List<WarenausgangItem> items = new ArrayList<>();
                            while (itemRS.next()) {
                                items.add(new WarenausgangItem(itemRS.getInt("productid"), itemRS.getInt("quantity")));
                            }
                            return new Warenausgang(wId, warenausgangDate, items);
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<Warenausgang> getAllWarenausgaenge() throws Exception {
        List<Warenausgang> warenausgaenge = new ArrayList<>();
        try (Connection connection = basicDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT warenausgangid FROM warenausgaenge");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int warenausgangId = rs.getInt("warenausgangid");
                Warenausgang w = getWarenausgang(warenausgangId);
                if (w != null) {
                    warenausgaenge.add(w);
                }
            }
        }
        return warenausgaenge;
    }

    @Override
    public void deleteWarenausgangTable() throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS warenausgaenge CASCADE;");
        }
    }

    @Override
    public void deleteWarenausgangItemsTable() throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS warenausgang_items CASCADE;");
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
