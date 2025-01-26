package com.example.fff.databse;

import com.example.fff.api.ProductManager;
import com.example.fff.api.WarenausgangManager;
import com.example.fff.api.WareneingangManager;
import com.example.fff.model.*;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Override
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
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();
            connection.setAutoCommit(false);
            LOGGER.log(Level.INFO, "Connection obtained and autoCommit set to false.");

            // 1) Warenausgang anlegen
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
            rs.close();
            warenausgangStmt.close();

            // 2) WarenausgangItems anlegen & Produktmengen reduzieren
            String insertWarenausgangItemSQL = "INSERT INTO warenausgang_items (warenausgangid, productid, quantity) VALUES (?, ?, ?)";
            warenausgangItemStmt = connection.prepareStatement(insertWarenausgangItemSQL);
            String updateProductSQL = "UPDATE products SET quantity = quantity - ? WHERE productid = ?";
            updateProductStmt = connection.prepareStatement(updateProductSQL);

            for (WarenausgangItem item : items) {
                warenausgangItemStmt.setInt(1, newWarenausgangId);
                warenausgangItemStmt.setInt(2, item.getProductId());
                warenausgangItemStmt.setInt(3, item.getQuantity());
                warenausgangItemStmt.addBatch();

                updateProductStmt.setInt(1, item.getQuantity());
                updateProductStmt.setInt(2, item.getProductId());
                updateProductStmt.addBatch();
            }

            warenausgangItemStmt.executeBatch();
            updateProductStmt.executeBatch();

            // 3) Transaktion abschließen
            connection.commit();
            LOGGER.log(Level.INFO, "Transaction committed for warenausgang {0}.", newWarenausgangId);

            // 4) Nach dem Commit automatische Nachbestellungen verarbeiten
            processAutomaticReorders(items, warenausgangDate);

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
            if (connection != null) connection.close();
            LOGGER.log(Level.INFO, "Resources closed in createWarenausgang().");
        }
    }


    private void processAutomaticReorders(List<WarenausgangItem> items, Timestamp referenceDate) {
        try {
            ProductManager productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
            WareneingangManager wareneingangManager = PostgresDBWareneingangManagement.getInstance();

            List<WareneingangItem> reorderItems = new ArrayList<>();

            // Prüfen für jedes Item, ob eine Nachbestellung nötig ist
            for (WarenausgangItem item : items) {
                Product updatedProduct = productManager.readProductById(item.getProductId());
                if (updatedProduct != null
                        && updatedProduct.getProductQuantity() < updatedProduct.getReorderPoint()) {
                    reorderItems.add(new WareneingangItem(
                            updatedProduct.getProductId(),
                            updatedProduct.getReorderQuantity()
                    ));
                }
            }

            if (!reorderItems.isEmpty()) {
                LOGGER.log(Level.INFO, "Erstelle automatischen Wareneingang für {0} Produkte.", reorderItems.size());
                // Einen separaten Wareneingang außerhalb der ursprünglichen Transaktion erstellen
                wareneingangManager.createWareneingang(reorderItems, referenceDate);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler bei der automatischen Nachbestellung: " + e.getMessage(), e);
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
                + "AND o.warenausgangdate >= NOW() - INTERVAL '14 days' "
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

    @Override
    public List<Warenausgang> getWarenausgaenge(Timestamp from, Timestamp to) throws Exception {
        List<Warenausgang> warenausgaenge = new ArrayList<>();
        String sql = "SELECT warenausgangid FROM warenausgaenge WHERE 1=1";
        if (from != null) {
            sql += " AND warenausgangdate >= ?";
        }
        if (to != null) {
            sql += " AND warenausgangdate <= ?";
        }

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
                    int warenausgangId = rs.getInt("warenausgangid");
                    Warenausgang w = getWarenausgang(warenausgangId);
                    if (w != null) {
                        warenausgaenge.add(w);
                    }
                }
            }
        }
        return warenausgaenge;
    }


    @Override
    public List<TagesStatistik> getWarenausgaengeProTag(Timestamp from, Timestamp to) throws Exception {
        List<TagesStatistik> statistikListe = new ArrayList<>();
        // Erweiterte SQL-Abfrage mit Summierung der Mengen:
        String sql = "SELECT DATE(w.warenausgangdate) AS tag, p.productname, SUM(wi.quantity) AS menge " +
                "FROM warenausgaenge w " +
                "JOIN warenausgang_items wi ON w.warenausgangid = wi.warenausgangid " +
                "JOIN products p ON wi.productid = p.productid " +
                "WHERE 1=1";

        if (from != null) {
            sql += " AND w.warenausgangdate >= ?";
        }
        if (to != null) {
            sql += " AND w.warenausgangdate <= ?";
        }
        sql += " GROUP BY DATE(w.warenausgangdate), p.productname " +
                "ORDER BY DATE(w.warenausgangdate), p.productname";

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
                    // Verwende "menge" anstelle von "anzahl"
                    statistikListe.add(new TagesStatistik(tag, menge, produktName));
                }
            }
        }
        return statistikListe;
    }


    @Override
    public boolean deleteWarenausgang(int warenausgangId) throws Exception {
        String sql = "DELETE FROM warenausgaenge WHERE warenausgangid = ?";
        try (Connection connection = basicDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, warenausgangId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new Exception("Error deleting warenausgang: " + e.getMessage(), e);
        }
    }

    @Override
    public double calculateSameDayHistoricalAverage(
            int productId,
            LocalDate targetDate,
            int years,
            Connection connection
    ) throws SQLException {
        // Wir summieren die Warenausgänge für genau diesen Kalendertag
        // in jedem der letzten `years` Jahre (z.B. 5).
        // targetDate ist z.B. "2025-01-20"

        double totalQuantity = 0.0;
        int countDaysFound = 0;

        String sql = """
        SELECT COALESCE(SUM(oi.quantity), 0) AS total_qty
        FROM warenausgaenge w
        JOIN warenausgang_items oi ON w.warenausgangid = oi.warenausgangid
        WHERE oi.productid = ?
          AND w.warenausgangdate >= ?
          AND w.warenausgangdate < ?
    """;

        for (int i = 1; i <= years; i++) {
            // 1) Bestimme das exakte Datum z.B. 20.01.(2025 - i)
            LocalDate dateInPast = targetDate.minusYears(i);

            // 2) Beginn dieses Tages
            Timestamp startOfDay = Timestamp.valueOf(dateInPast.atStartOfDay());
            // 3) Beginn des Folgetages (Ausschlussgrenze)
            Timestamp endOfDay = Timestamp.valueOf(dateInPast.plusDays(1).atStartOfDay());

            // 4) Query ausführen
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, productId);
                ps.setTimestamp(2, startOfDay);
                ps.setTimestamp(3, endOfDay);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double qty = rs.getDouble("total_qty");
                        // Nur wenn tatsächlich ein Warenausgang > 0 stattfand,
                        // könnte man wahlweise den Tag zählen.
                        // Hier: Wir addieren die gefundene Menge.
                        // Ggf. kann man bei qty=0 unterscheiden, ob man den Tag ignoriert.
                        totalQuantity += qty;
                        countDaysFound++;
                    }
                }
            }
        }

        // Durchschnitt bilden
        if (countDaysFound == 0) {
            return 0.0;
        }
        return totalQuantity / countDaysFound;
    }

    @Override
    public double calculateDemandInPeriod(int productId, LocalDate from, LocalDate to, Connection conn) throws SQLException {
        String sql = """
        SELECT COALESCE(SUM(wai.quantity), 0) AS total
          FROM warenausgangitems wai
          JOIN warenausgang wa ON wai.warenausgangid = wa.id
         WHERE wai.productid = ?
           AND wa.warenausgangdate >= ?
           AND wa.warenausgangdate < ?
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            // from inclusive
            ps.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
            // to exclusive => +1 Tag
            ps.setTimestamp(3, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

}
