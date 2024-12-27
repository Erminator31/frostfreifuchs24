package com.example.fff.databse;

import com.example.fff.api.WareneingangManager;
import com.example.fff.model.Wareneingang;
import com.example.fff.model.WareneingangItem;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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
                    "wareneingangdate TIMESTAMP NOT NULL DEFAULT NOW()" +
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
        return createWareneingang(items, new Timestamp(System.currentTimeMillis()));
    }

    @Override
    public Wareneingang createWareneingang(List<WareneingangItem> items, Timestamp wareneingangDate) throws Exception {
        Connection connection = null;
        PreparedStatement wEinStmt = null;
        PreparedStatement wEinItemStmt = null;
        ResultSet rs = null;

        try {
            connection = basicDataSource.getConnection();
            connection.setAutoCommit(false);

            String insertWEinSQL = "INSERT INTO wareneingaenge (wareneingangdate) VALUES (?) RETURNING wareneingangid, wareneingangdate";
            wEinStmt = connection.prepareStatement(insertWEinSQL);
            wEinStmt.setTimestamp(1, wareneingangDate);
            rs = wEinStmt.executeQuery();

            int newWareneingangId = -1;
            Timestamp returnedDate = null;
            if (rs.next()) {
                newWareneingangId = rs.getInt("wareneingangid");
                returnedDate = rs.getTimestamp("wareneingangdate");
            }

            if (newWareneingangId == -1) {
                throw new SQLException("Could not create wareneingang");
            }

            String insertItemSQL = "INSERT INTO wareneingang_items (wareneingangid, productid, quantity) VALUES (?,?,?)";
            wEinItemStmt = connection.prepareStatement(insertItemSQL);

            for (WareneingangItem item : items) {
                wEinItemStmt.setInt(1, newWareneingangId);
                wEinItemStmt.setInt(2, item.getProductId());
                wEinItemStmt.setInt(3, item.getQuantity());
                wEinItemStmt.addBatch();

                // Increase stock
                String updateProduct = "UPDATE products SET quantity = quantity + ? WHERE productid = ?";
                try (PreparedStatement ps = connection.prepareStatement(updateProduct)) {
                    ps.setInt(1, item.getQuantity());
                    ps.setInt(2, item.getProductId());
                    ps.executeUpdate();
                }
            }

            wEinItemStmt.executeBatch();
            connection.commit();

            return new Wareneingang(newWareneingangId, returnedDate.toString(), items);

        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
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
            String selectWEin = "SELECT wareneingangid, wareneingangdate FROM wareneingaenge WHERE wareneingangid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(selectWEin)) {
                stmt.setInt(1, wareneingangId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    int wId = rs.getInt("wareneingangid");
                    String date = rs.getString("wareneingangdate");

                    String selectItems = "SELECT productid, quantity FROM wareneingang_items WHERE wareneingangid = ?";
                    try (PreparedStatement itemStmt = connection.prepareStatement(selectItems)) {
                        itemStmt.setInt(1, wId);
                        try (ResultSet irs = itemStmt.executeQuery()) {
                            List<WareneingangItem> items = new ArrayList<>();
                            while (irs.next()) {
                                items.add(new WareneingangItem(irs.getInt("productid"), irs.getInt("quantity")));
                            }
                            return new Wareneingang(wId, date, items);
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
}
