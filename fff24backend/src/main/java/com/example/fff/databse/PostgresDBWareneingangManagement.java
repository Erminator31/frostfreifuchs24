package com.example.fff.databse;

import com.example.fff.api.WareneingangManager;
import com.example.fff.model.TagesStatistik;
import com.example.fff.model.Wareneingang;
import com.example.fff.model.WareneingangItem;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PostgresDBWareneingangManagement implements WareneingangManager {

    String databaseURL = "jdbc:postgresql://c7u1tn6bvvsodf.cluster-czz5s0kz4scl.eu-west-1.rds.amazonaws.com:5432/d1t207hd56v54?sslmode=require";
    String username = "u3t73itv4ifknl";
    String password = "pc8d79bc3deea2ca2b99f04d14057aeb257bac911861af2c1c3f890ffecaa803c";

    BasicDataSource basicDataSource;

    private static PostgresDBWareneingangManagement instance = null;

    private static final Logger LOGGER = Logger.getLogger(PostgresDBWareneingangManagement.class.getName());

    private PostgresDBWareneingangManagement() {
        basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(databaseURL);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
    }

    public static PostgresDBWareneingangManagement getInstance() {
        if (instance == null) {
            instance = new PostgresDBWareneingangManagement();
        }
        return instance;
    }

    @Override
    public void createWareneingangTable() throws Exception {
        try (Connection conn = basicDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS wareneingaenge (" +
                    "wareneingangid SERIAL PRIMARY KEY, " +
                    "wareneingangdate TIMESTAMP NOT NULL DEFAULT NOW(), " +
                    "wareneingangmode VARCHAR(50) DEFAULT 'manual'" +
                    ");";
            stmt.execute(sql);
        }
    }


    @Override
    public void createWareneingangItemTable() throws Exception {
        try (Connection conn = basicDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS wareneingang_items (" +
                    "wareneingangitemid SERIAL PRIMARY KEY, " +
                    "wareneingangid INT NOT NULL, " +
                    "productid INT NOT NULL, " +
                    "quantity INT DEFAULT NULL, " +
                    "FOREIGN KEY (wareneingangid) REFERENCES wareneingaenge(wareneingangid) ON DELETE CASCADE, " +
                    "FOREIGN KEY (productid) REFERENCES products(productid) ON DELETE CASCADE" +
                    ");";
            stmt.execute(sql);
        }
    }

    @Override
    public Wareneingang createWareneingang(List<WareneingangItem> items) throws Exception {
        return createWareneingang(items, new Timestamp(System.currentTimeMillis()),"manual");
    }

    @Override
    public Wareneingang createWareneingang(List<WareneingangItem> items, Timestamp wareneingangDate) throws Exception {
        return createWareneingang(items, wareneingangDate,"manual" );
    }
    @Override
    public Wareneingang createWareneingang(List<WareneingangItem> items, Timestamp wareneingangDate, String mode) throws Exception {
        LOGGER.log(Level.INFO,
                "createWareneingang called with {0} items, date={1}, mode={2}",
                new Object[]{ items.size(), wareneingangDate,mode });

        Connection connection = null;
        PreparedStatement wEinStmt = null;
        PreparedStatement wEinItemStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();
            connection.setAutoCommit(false);
            LOGGER.log(Level.INFO, "Connection obtained, autoCommit set to false.");

            // 1) Check current capacity
            String sumSQL = "SELECT COALESCE(SUM(quantity), 0) AS total_quantity FROM products";
            int currentTotalQuantity = 0;
            try (PreparedStatement sumStmt = connection.prepareStatement(sumSQL);
                 ResultSet sumRs = sumStmt.executeQuery()) {
                if (sumRs.next()) {
                    currentTotalQuantity = sumRs.getInt("total_quantity");
                }
            }
            int remainingCapacity = 4000 - currentTotalQuantity;
            LOGGER.log(Level.INFO, "Aktueller Gesamtbestand: {0}, verbleibende Kapazität: {1}",
                    new Object[]{ currentTotalQuantity, remainingCapacity });

            // 2) Adjust incoming quantities based on available capacity
            List<WareneingangItem> adjustedItems = new ArrayList<>();
            for (WareneingangItem item : items) {
                if (remainingCapacity <= 0) {
                    break;  // no more capacity
                }
                int quantityToAdd = Math.min(item.getQuantity(), remainingCapacity);
                if (quantityToAdd > 0) {
                    adjustedItems.add(new WareneingangItem(item.getProductId(), quantityToAdd));
                    remainingCapacity -= quantityToAdd;
                }
            }
            if (adjustedItems.isEmpty()) {
                connection.rollback();
                throw new Exception("Keine Kapazität mehr für Wareneingänge (Lager ist voll: 4.000).");
            }

            // 3) Insert the wareneingang row (with wareneingangmode)
            String insertWEinSQL =
                    "INSERT INTO wareneingaenge (wareneingangdate, wareneingangmode) " +
                            "VALUES (?, ?) RETURNING wareneingangid, wareneingangdate, wareneingangmode";

            wEinStmt = connection.prepareStatement(insertWEinSQL);
            wEinStmt.setTimestamp(1, wareneingangDate);
            wEinStmt.setString(2, mode);

            rs = wEinStmt.executeQuery();
            int newWareneingangId = -1;
            Timestamp returnedDate = null;
            String returnedMode = null;

            if (rs.next()) {
                newWareneingangId = rs.getInt("wareneingangid");
                returnedDate      = rs.getTimestamp("wareneingangdate");
                returnedMode      = rs.getString("wareneingangmode");
            }
            if (newWareneingangId == -1) {
                throw new SQLException("Could not create wareneingang.");
            }

            rs.close();
            wEinStmt.close();

            // 4) Insert wareneingang items
            String insertItemSQL =
                    "INSERT INTO wareneingang_items (wareneingangid, productid, quantity) VALUES (?,?,?)";
            wEinItemStmt = connection.prepareStatement(insertItemSQL);

            for (WareneingangItem item : adjustedItems) {
                wEinItemStmt.setInt(1, newWareneingangId);
                wEinItemStmt.setInt(2, item.getProductId());
                wEinItemStmt.setInt(3, item.getQuantity());
                wEinItemStmt.addBatch();

                // Update product quantity
                String updateProduct =
                        "UPDATE products SET quantity = quantity + ? WHERE productid = ?";
                try (PreparedStatement ps = connection.prepareStatement(updateProduct)) {
                    ps.setInt(1, item.getQuantity());
                    ps.setInt(2, item.getProductId());
                    ps.executeUpdate();
                }
            }
            wEinItemStmt.executeBatch();

            // 5) Commit
            connection.commit();
            LOGGER.log(Level.INFO, "Transaction committed for wareneingang {0}.", newWareneingangId);

            // 6) Return Wareneingang object
            return new Wareneingang(
                    newWareneingangId,
                    (returnedDate != null ? returnedDate.toString() : wareneingangDate.toString()),
                    adjustedItems,
                    returnedMode
            );

        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
                LOGGER.log(Level.SEVERE,
                        "Exception in createWareneingang, rolling back: {0}", e.getMessage());
            }
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (wEinStmt != null) wEinStmt.close();
            if (wEinItemStmt != null) wEinItemStmt.close();
            if (connection != null) connection.close();
        }
    }



    @Override
    public Wareneingang getWareneingang(int wareneingangId) throws Exception {
        try (Connection connection = basicDataSource.getConnection()) {
            String selectWEin = "SELECT wareneingangid, wareneingangdate, wareneingangmode " +
                    "FROM wareneingaenge WHERE wareneingangid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(selectWEin)) {
                stmt.setInt(1, wareneingangId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    int wId            = rs.getInt("wareneingangid");
                    String date        = rs.getString("wareneingangdate");
                    String mode        = rs.getString("wareneingangmode");

                    String selectItems = "SELECT productid, quantity FROM wareneingang_items WHERE wareneingangid = ?";
                    try (PreparedStatement itemStmt = connection.prepareStatement(selectItems)) {
                        itemStmt.setInt(1, wId);
                        try (ResultSet irs = itemStmt.executeQuery()) {
                            List<WareneingangItem> items = new ArrayList<>();
                            while (irs.next()) {
                                items.add(new WareneingangItem(
                                        irs.getInt("productid"),
                                        irs.getInt("quantity")
                                ));
                            }
                            return new Wareneingang(wId, date, items, mode);
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<Wareneingang> getAllWareneingaenge() throws Exception {
        List<Wareneingang> eingange = new ArrayList<>();
        try (Connection connection = basicDataSource.getConnection()) {
            String selectAll = "SELECT wareneingangid FROM wareneingaenge ORDER BY wareneingangdate DESC";
            try (PreparedStatement stmt = connection.prepareStatement(selectAll);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int wId = rs.getInt("wareneingangid");
                    Wareneingang w = getWareneingang(wId);
                    if (w != null) {
                        eingange.add(w);
                    }
                }
            }
        }
        return eingange;
    }

    @Override
    public void deleteWareneingangTable() throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS wareneingaenge CASCADE;");
        }
    }

    @Override
    public void deleteWareneingangItemsTable() throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS wareneingang_items CASCADE;");
        }
    }

    @Override
    public List<TagesStatistik> getWareneingaengeProTag(Timestamp from, Timestamp to) throws Exception {
        List<TagesStatistik> statistikListe = new ArrayList<>();
        String sql = "SELECT DATE(wg.wareneingangdate) AS tag, p.productname, SUM(wi.quantity) AS menge " +
                "FROM wareneingaenge wg " +
                "JOIN wareneingang_items wi ON wg.wareneingangid = wi.wareneingangid " +
                "JOIN products p ON wi.productid = p.productid " +
                "WHERE 1=1";

        if (from != null) {
            sql += " AND wg.wareneingangdate >= ?";
        }
        if (to != null) {
            sql += " AND wg.wareneingangdate <= ?";
        }
        sql += " GROUP BY DATE(wg.wareneingangdate), p.productname " +
                "ORDER BY DATE(wg.wareneingangdate), p.productname";

        try (Connection connection = basicDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            int paramIndex = 1;
            if (from != null) {
                stmt.setTimestamp(paramIndex++, from);
            }
            if (to != null) {
                stmt.setTimestamp(paramIndex++, to);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tag = rs.getString("tag");
                    String produktName = rs.getString("productname");
                    int menge = rs.getInt("menge");
                    statistikListe.add(new TagesStatistik(tag, menge, produktName));
                }
            }
        }
        return statistikListe;
    }


}
