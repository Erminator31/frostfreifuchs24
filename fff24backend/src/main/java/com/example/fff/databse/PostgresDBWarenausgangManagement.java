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

/**
 * This class, PostgresDBWarenausgangManagement, is responsible for managing
 * the database operations related to "warenausgaenge" (goods outgoing) in a PostgreSQL database.
 * It provides methods to create and manage tables, handle records for warenausgaenge and their
 * associated items, and process inventory-related tasks. The class implements a singleton
 * pattern to ensure a single instance across the application.
 */
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

    /**
     * Provides a singleton instance of the PostgresDBWarenausgangManagement class.
     * This ensures that only one instance of the class is created and used throughout the application.
     *
     * @return the singleton instance of PostgresDBWarenausgangManagement
     */
    public static PostgresDBWarenausgangManagement getInstance() {
        if (instance == null) {
            instance = new PostgresDBWarenausgangManagement();
        }
        return instance;
    }

    /**
     * Retrieves the data source associated with the PostgresDBWarenausgangManagement.
     *
     * @return the BasicDataSource instance used for database connections
     */
    @Override
    public BasicDataSource getDataSource() {
        return basicDataSource;
    }
    /**
     * Creates the `warenausgaenge` table in the database if it does not already exist.
     * The table contains the following columns:
     * - `warenausgangid`: A unique identifier for each record, defined as a SERIAL primary key.
     * - `warenausgangdate`: A timestamp column that records the date and time of the record,
     *   with a default value of the current timestamp.
     * - `quantity`: An integer column representing the quantity, which is optional and can be null.
     *
     * This method establishes a connection to the database using the provided data source,
     * executes the SQL command to create the table, and ensures that it follows the defined schema.
     *
     * @throws Exception if there is an issue connecting to the database or executing the table creation statement.
     */
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

    /**
     * Creates the "warenausgang_items" table in the database if it does not already exist.
     *
     * This table is used to store the items associated with warenausgaenge (goods outgoing).
     * It contains fields for the item ID, the warenausgang ID to which it is linked,
     * the product ID of the item, and its quantity. Foreign key relationships are defined
     * for the "warenausgangid" and "productid" fields, ensuring referential integrity with
     * the corresponding "warenausgaenge" and "products" tables. The foreign key constraints
     * include cascading deletions.
     *
     * The method utilizes a database connection from the configured data source and executes
     * a SQL statement to create the table. If a SQL error occurs during the process,
     * an exception is thrown with a detailed error message.
     *
     * @throws Exception if an error occurs while creating the table in the database
     */
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

    /**
     * Creates a new instance of Warenausgang based on the given list of WarenausgangItem objects.
     * This method uses the current system timestamp as the Warenausgang date.
     *
     * @param items the list of items to include in the created Warenausgang. Each item specifies
     *              details such as the product ID and the quantity.
     * @return the created Warenausgang object, which includes a unique ID, the current timestamp
     *         as the Warenausgang date, and the provided list of items.
     * @throws Exception if an error occurs during the creation of the Warenausgang.
     */
    @Override
    public Warenausgang createWarenausgang(List<WarenausgangItem> items) throws Exception {
        return createWarenausgang(items, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Creates a new Warenausgang (goods outgoing) record in the database, along with its associated items,
     * and updates the inventory levels for the respective products. This method performs the following:
     * 1. Inserts a new Warenausgang record with the provided timestamp.
     * 2. Inserts Warenausgang items linked to the created record.
     * 3. Adjusts inventory levels for the corresponding products.
     * 4. Processes automatic reorders based on the outgoing items.
     *
     * @param items the list of WarenausgangItem objects, each representing a product and its quantity to be recorded
     * @param warenausgangDate the timestamp indicating the date and time of the Warenausgang record
     * @return a Warenausgang object representing the created record, including its ID, date, and associated items
     * @throws Exception if an error occurs during database operations or transaction processing
     */
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


    /**
     * Processes automatic reorders based on the given list of WarenausgangItem objects.
     * If the quantity of a product falls below its reorder point, the method generates
     * a corresponding reorder item to replenish stock levels. The reorders are logged
     * and processed with a reference date.
     *
     * @param items          the list of WarenausgangItem objects representing goods outgoing.
     *                       Each item includes details such as the product ID and quantity.
     * @param referenceDate  the timestamp used as the reference date for creating the reorders.
     */
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
                wareneingangManager.createWareneingang(reorderItems, referenceDate,"automatic");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler bei der automatischen Nachbestellung: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a specific Warenausgang (goods outgoing) record from the database based on its ID.
     * The method fetches the Warenausgang details, including its associated items, and returns a
     * Warenausgang object representing the record.
     *
     * @param warenausgangId the unique identifier of the Warenausgang record to be retrieved
     * @return the Warenausgang object containing the ID, date, and associated items;
     *         or null if no record is found
     * @throws Exception if an error occurs while accessing the database or executing the query
     */
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

    /**
     * Retrieves all Warenausgang (goods outgoing) records from the database.
     * This method fetches the list of all Warenausgang entries by querying the database for their IDs,
     * then retrieves each individual Warenausgang object along with its associated details.
     *
     * @return a list of Warenausgang objects representing the goods outgoing records.
     *         Each object includes the Warenausgang ID, date, and associated items.
     * @throws Exception if an error occurs during the database query or retrieval process.
     */
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

    /**
     * Deletes the `warenausgaenge` table from the database if it exists.
     * This method drops the table along with its dependencies by using the
     * `CASCADE` option. The operation ensures that all associated data and
     * constraints are removed.
     *
     * A connection to the database is obtained from the data source, and
     * the SQL statement `DROP TABLE IF EXISTS` is executed. After execution,
     * the connection and statement are automatically closed using
     * try-with-resources.
     *
     * @throws SQLException if there is an error while connecting to the database
     *                       or executing the SQL statement.
     */
    @Override
    public void deleteWarenausgangTable() throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS warenausgaenge CASCADE;");
        }
    }

    /**
     * Deletes the `warenausgang_items` table from the database, if it exists.
     *
     * This method drops the table along with its dependencies by using the `CASCADE` option.
     * The operation ensures that all associated data and constraints are removed, including
     * any foreign key relationships.
     *
     * The method establishes a database connection from the configured data source and
     * executes the SQL `DROP TABLE IF EXISTS` command. After execution, the connection
     * and statement are automatically closed using try-with-resources.
     *
     * @throws SQLException if there is an error while connecting to the database or
     *                      executing the SQL statement.
     */
    @Override
    public void deleteWarenausgangItemsTable() throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS warenausgang_items CASCADE;");
        }
    }

    /**
     * Calculates the average daily demand for a specific product based on warehouse
     * dispatch records from the last 14 days.
     *
     * @param productId the ID of the product for which to calculate the average daily demand
     * @param connection the database connection used to retrieve the dispatch records
     * @return the average daily demand for the product over the last 14 days; returns 0.0
     *         if no records are found
     * @throws SQLException if a database access error occurs
     */
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

    /**
     * Retrieves a list of Warenausgang objects within the specified date range.
     *
     * @param from the start timestamp; can be null to include all records from the earliest date
     * @param to the end timestamp; can be null to include all records up to the latest date
     * @return a list of Warenausgang objects that fall within the specified date range
     * @throws Exception if a database access error occurs or the query execution fails
     */
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


    /**
     * Fetches the daily statistics of shipped goods within a specified date range. The method
     * retrieves the total quantity of each product shipped per day, grouped by date and product name.
     *
     * @param from The starting timestamp for the date range filter. If null, no lower bound is applied.
     * @param to The ending timestamp for the date range filter. If null, no upper bound is applied.
     * @return A list of TagesStatistik objects, each containing the date, product name,
     *         and total quantity of the product shipped on that day.
     * @throws Exception If a database access error occurs or the SQL query execution fails.
     */
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
                    statistikListe.add(new TagesStatistik(tag, menge, produktName));
                }
            }
        }
        return statistikListe;
    }


    /**
     * Deletes a Warenausgang entry from the database based on the provided ID.
     *
     * @param warenausgangId the ID of the Warenausgang to be deleted
     * @return true if the Warenausgang was successfully deleted, false otherwise
     * @throws Exception if a database access error occurs or the deletion operation fails
     */
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

    /**
     * Calculates the historical average quantity of product exits for the same calendar day
     * in each of the last specified years.
     *
     * This method sums up the quantities of product exits that occurred on the same day
     * (ignoring the year) as the provided target date across the specified number of previous years.
     * If no data is found for the product on the specified day in past years, the method returns 0.0.
     *
     * @param productId the ID of the product for which the average is being calculated
     * @param targetDate the target date to compare against in the calculation (e.g., "2025-01-20")
     * @param years the number of past years to include in the calculation
     * @param connection the database connection used to query the historical product data
     * @return the average quantity of product exits on the same calendar day across the specified years
     * @throws SQLException if a database access error occurs
     */
    @Override
    public double calculateSameDayHistoricalAverage(
            int productId,
            LocalDate targetDate,
            int years,
            Connection connection
    ) throws SQLException {


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

    /**
     * Calculates the total demand for a given product within a specified time period.
     *
     * @param productId The ID of the product for which demand is to be calculated.
     * @param from The start date (inclusive) of the period.
     * @param to The end date (exclusive) of the period.
     * @param conn The database connection to be used for the query.
     * @return The total demand for the product in the specified period as a double value.
     * @throws SQLException If a database access error occurs.
     */
    @Override
    public double calculateDemandInPeriod(int productId, LocalDate from, LocalDate to, Connection conn) throws SQLException {
        String sql = """
        SELECT COALESCE(SUM(wai.quantity), 0) AS total
          FROM warenausgang_items wai
          JOIN warenausgaenge wa ON wai.warenausgangid = wa.warenausgangid
         WHERE wai.productid = ?
           AND wa.warenausgangdate >= ?
           AND wa.warenausgangdate < ?
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setTimestamp(2, Timestamp.valueOf(from.atStartOfDay()));
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
