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

/**
 * The {@code PostgresDBWareneingangManagement} class is responsible for managing
 * Wareneingang (goods receipt) data in a PostgreSQL database. It provides methods
 * for creating, retrieving, and deleting Wareneingang records and their associated items,
 * as well as generating statistics related to goods receipts.
 *
 * This class implements the {@code WareneingangManager} interface from the
 * {@code com.example.fff.api} package, ensuring compatibility with the broader
 * application structure.
 *
 * Features include:
 * - Singleton instance management to ensure a single point of database access.
 * - Dynamic creation of database tables for Wareneingang and associated items.
 * - Operations for creating and retrieving Wareneingang entries, with support for
 *   custom timestamps and modes.
 * - Ensuring referential integrity between tables such as Wareneingang, WareneingangItems,
 *   and Products.
 * - Robust error handling for database interactions.
 */
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

    /**
     * Provides a singleton instance of the {@code PostgresDBWareneingangManagement} class.
     * Ensures that only one instance of the class is created and reused throughout the application.
     *
     * @return the singleton instance of {@code PostgresDBWareneingangManagement}
     */
    public static PostgresDBWareneingangManagement getInstance() {
        if (instance == null) {
            instance = new PostgresDBWareneingangManagement();
        }
        return instance;
    }

    /**
     * Creates the table `wareneingaenge` in the database if it does not already exist.
     *
     * The table `wareneingaenge` contains the following columns:
     * - `wareneingangid`: A unique identifier for each wareneingang, defined as a SERIAL primary key.
     * - `wareneingangdate`: A timestamp indicating the date and time of the wareneingang. Defaults to the current timestamp if not specified.
     * - `wareneingangmode`: A varchar field representing the mode of wareneingang, with a default value of 'manual'.
     *
     * This method establishes a connection to the database using a preconfigured connection pool and executes the SQL statement to create the table if it does not already exist.
     *
     * @throws Exception if a database connection error occurs or the SQL execution fails.
     */
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


    /**
     * Creates the database table `wareneingang_items` if it does not exist.
     *
     * The `wareneingang_items` table is used to store items associated with a
     * specific incoming goods (wareneingang). Each entry in the table associates
     * a product with a specific entry in the `wareneingaenge` table and defines
     * the quantity of the product in the goods receipt.
     *
     * Table Structure:
     * - `wareneingangitemid`: A unique identifier for the item (Primary Key, Auto-incremented).
     * - `wareneingangid`: A foreign key referencing the `wareneingaenge` table. Ensures that
     *   the item is associated with a valid incoming goods entry. Cascade delete is applied.
     * - `productid`: A foreign key referencing the `products` table. Ensures that the item
     *   references a valid product. Cascade delete is applied.
     * - `quantity`: The quantity of the product in this goods receipt (can be null).
     *
     * Ensures referential integrity between the `wareneingang_items` table,
     * the `wareneingaenge` table, and the `products` table.
     *
     * @throws Exception if a database connection issue occurs or the SQL fails to execute.
     */
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

    /**
     * Creates a new Wareneingang with the specified list of items.
     * The method uses the current system timestamp and sets the mode to "manual".
     *
     * @param items the list of WareneingangItem objects to be added to the Wareneingang
     * @return the created Wareneingang object containing details of the added items, date, and mode
     * @throws Exception if an error occurs during the creation process or if the storage capacity is exceeded
     */
    @Override
    public Wareneingang createWareneingang(List<WareneingangItem> items) throws Exception {
        return createWareneingang(items, new Timestamp(System.currentTimeMillis()),"manual");
    }

    /**
     * Creates a new Wareneingang using the provided list of WareneingangItem objects and the given Wareneingang timestamp.
     * The method will adjust quantities if existing product capacity constraints are exceeded and log relevant operations.
     * If the warehouse is full, an exception is thrown.
     *
     * @param items                     List of WareneingangItem objects representing the products and their quantities.
     * @param wareneingangDate          Timestamp indicating the date and time of the Wareneingang.
     * @return A Wareneingang object representing the successfully created entry, including its items and mode.
     * @throws Exception                If any error occurs during the creation of Wareneingang or database operations.
     */
    @Override
    public Wareneingang createWareneingang(List<WareneingangItem> items, Timestamp wareneingangDate) throws Exception {
        return createWareneingang(items, wareneingangDate,"manual" );
    }
    /**
     * Creates a new Wareneingang (goods receipt) entry in the database, including its associated items.
     * Ensures that incoming quantities are adjusted based on available storage capacity and updates
     * product stock accordingly.
     *
     * @param items the list of {@code WareneingangItem} representing the products and quantities in the receipt
     * @param wareneingangDate the timestamp of the Wareneingang
     * @param mode a string representing the mode associated with this Wareneingang
     * @return a {@code Wareneingang} object containing the details of the created Wareneingang, including its ID, date, items, and mode
     * @throws Exception if there is an error while creating the Wareneingang
     */
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



    /**
     * Retrieves a Wareneingang (goods receipt) based on the provided Wareneingang ID.
     * Queries the database to fetch details of the Wareneingang, including its associated items.
     *
     * @param wareneingangId the unique identifier of the Wareneingang to be retrieved
     * @return the Wareneingang object corresponding to the given ID, or null if no record is found
     * @throws Exception if a database access error occurs or the query execution fails
     */
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

    /**
     * Fetches and returns a list of all Wareneingang entries from the database in descending order
     * of their entry date. Each Wareneingang entry is fully populated with its associated details.
     * If an entry cannot be retrieved or is null, it is excluded from the final list.
     *
     * A database connection is established to query the `wareneingaenge` table,
     * ensuring efficient handling of resources using a connection pool.
     *
     * @return a list of {@code Wareneingang} objects containing the details of all goods receipts
     *         (wareneingaenge) currently stored in the database.
     * @throws Exception if there is any issue with database connection or SQL execution.
     */
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

    /**
     * Deletes the `wareneingaenge` table from the database, if it exists.
     *
     * This method establishes a connection to the database using the configured
     * connection pool and executes an SQL command to drop the `wareneingaenge` table.
     * If the table does not exist, the method does not throw an error due to the
     * use of the `IF EXISTS` condition in the SQL statement. Cascade behavior is applied
     * to ensure any dependent objects are also removed.
     *
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void deleteWareneingangTable() throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS wareneingaenge CASCADE;");
        }
    }

    /**
     * Deletes the `wareneingang_items` table from the database if it exists.
     *
     * This method establishes a connection to the database, executes the SQL command
     * to drop the table `wareneingang_items`, and ensures that related data in
     * dependent tables is also removed using the CASCADE option.
     *
     * The method leverages a preconfigured connection pool to get a database connection
     * and uses a try-with-resources block to ensure proper resource management.
     *
     * @throws SQLException if a database access error occurs or the operation fails
     */
    @Override
    public void deleteWareneingangItemsTable() throws SQLException {
        try (Connection connection = basicDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS wareneingang_items CASCADE;");
        }
    }

    /**
     * Retrieves a list of daily statistics for incoming goods (Wareneingaenge)
     * in a specified time range. Each entry in the list represents the total
     * quantity of a specific product received on a particular day.
     *
     * @param from the start timestamp specifying the beginning of the time range; can be null for no lower bound
     * @param to the end timestamp specifying the end of the time range; can be null for no upper bound
     * @return a list of {@code TagesStatistik} objects containing daily statistics of incoming goods,
     * each with date, product name, and total quantity
     * @throws Exception if a database access error occurs or query execution fails
     */
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
