package com.example.fff.databse;

import com.example.fff.api.ProductManager;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class PostgresDBProductManagement implements ProductManager {
    // Database connection details
    String databaseURL = "jdbc:postgresql://ec2-34-235-108-214.compute-1.amazonaws.com:5432/d6knq5qjvgg4o1?sslmode=require";
    String username = "rbkydptdnppvdl";
    String password = "95001e652dc225ee9ecde073bdeb56f7d9f62a4db04a53a0e0dc470048f373d5";
    BasicDataSource basicDataSource;

    // Singleton pattern for the manager implementation
    private static PostgresDBProductManagement postgresDBProductManagement = null;

    private static final Logger LOGGER = Logger.getLogger(PostgresDBProductManagement.class.getName());

    /**
     * Private constructor initializes the data source.
     */
    private PostgresDBProductManagement() {
        basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(databaseURL);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
    }

    /**
     * Singleton pattern: Get an instance of this manager.
     * Create it if it doesn't exist.
     *
     * @return instance of PostgresDBEventManagerImpl
     */
    public static PostgresDBProductManagement getPostgresDBEventManagerImpl() {
        if (postgresDBProductManagement == null) {
            postgresDBProductManagement = new PostgresDBProductManagement();
        }
        return postgresDBProductManagement;
    }

    /**
     * Creates an 'events' table in the database if it doesn't already exist.
     *
     * @throws Exception if there's any issue creating the table
     */
    @Override
    public void createProductTable() throws Exception {
        Connection connection = null;
        PreparedStatement pstmt = null;
        String createTableSQL = "CREATE TABLE IF NOT EXISTS events (" +
                "product_id SERIAL PRIMARY KEY, " +
                "product_name VARCHAR(255) NOT NULL, " +
                "product_type VARCHAR(100) NOT NULL, ";

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


}
