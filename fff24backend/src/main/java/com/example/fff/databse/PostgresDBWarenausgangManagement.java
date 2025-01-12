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
import java.util.logging.Level;
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

    public BasicDataSource getDataSource() {
        return basicDataSource;
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
        LOGGER.log(Level.INFO,
                "createWarenausgang called with {0} items, timestamp={1}",
                new Object[] { items.size(), warenausgangDate });

        Connection connection = null;
        PreparedStatement warenausgangStmt = null;
        PreparedStatement warenausgangItemStmt = null;
        PreparedStatement updateProductStmt = null;
        PreparedStatement updateReorderStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();
            connection.setAutoCommit(false);
            LOGGER.log(Level.INFO, "Connection obtained and autoCommit set to false.");

            // Insert Warenausgang
            String insertWarenausgangSQL = "INSERT INTO warenausgaenge (warenausgangdate) VALUES (?) RETURNING warenausgangid, warenausgangdate";
            warenausgangStmt = connection.prepareStatement(insertWarenausgangSQL);
            warenausgangStmt.setTimestamp(1, warenausgangDate);
            LOGGER.log(Level.INFO, "Executing SQL: {0}", insertWarenausgangSQL);

            rs = warenausgangStmt.executeQuery();
            int newWarenausgangId = -1;
            Timestamp returnedDate = null;
            if (rs.next()) {
                newWarenausgangId = rs.getInt("warenausgangid");
                returnedDate = rs.getTimestamp("warenausgangdate");
            }
            LOGGER.log(Level.INFO, "Inserted new Warenausgang with ID={0}, date={1}",
                    new Object[]{ newWarenausgangId, returnedDate });

            if (newWarenausgangId == -1) {
                throw new SQLException("Could not create warenausgang");
            }

            rs.close();
            warenausgangStmt.close();

            // Prepare item insert
            String insertWarenausgangItemSQL = "INSERT INTO warenausgang_items (warenausgangid, productid, quantity) VALUES (?, ?, ?)";
            warenausgangItemStmt = connection.prepareStatement(insertWarenausgangItemSQL);

            String updateProductSQL = "UPDATE products SET quantity = quantity - ? WHERE productid = ?";
            updateProductStmt = connection.prepareStatement(updateProductSQL);

            for (WarenausgangItem item : items) {
                // Insert Warenausgang item
                warenausgangItemStmt.setInt(1, newWarenausgangId);
                warenausgangItemStmt.setInt(2, item.getProductId());
                warenausgangItemStmt.setInt(3, item.getQuantity());
                warenausgangItemStmt.addBatch();

                // Decrease product stock
                updateProductStmt.setInt(1, item.getQuantity());
                updateProductStmt.setInt(2, item.getProductId());
                updateProductStmt.addBatch();

                LOGGER.log(Level.INFO,
                        "Batching item insert for productId={0}, quantity={1} and product update (subtract).",
                        new Object[]{ item.getProductId(), item.getQuantity() });
            }

            warenausgangItemStmt.executeBatch();
            LOGGER.log(Level.INFO, "Warenausgang items inserted via batch.");

            updateProductStmt.executeBatch();
            LOGGER.log(Level.INFO, "Product quantities updated via batch.");

            // Then reorder logic, etc...
            // If you're calling reorder or Wareneingang code, add logs there as well.

            connection.commit();
            LOGGER.log(Level.INFO, "Transaction committed for warenausgang {0}.", newWarenausgangId);

            return new Warenausgang(newWarenausgangId, returnedDate.toString(), items);

        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
                LOGGER.log(Level.SEVERE, "Exception in createWarenausgang, rolling back: {0}", e.getMessage());
            }
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (warenausgangStmt != null) warenausgangStmt.close();
            if (warenausgangItemStmt != null) warenausgangItemStmt.close();
            if (updateProductStmt != null) updateProductStmt.close();
            if (updateReorderStmt != null) updateReorderStmt.close();
            if (connection != null) connection.close();
            LOGGER.log(Level.INFO, "Resources closed in createWarenausgang().");
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
                + "AND o.warenausgangdate >= DATEADD(DAY, -14, GETDATE()) "
                + "ORDER BY o.warenausgangdate DESC ";

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
