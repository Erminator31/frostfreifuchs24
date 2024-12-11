package com.example.fff.api;

import com.example.fff.model.Product;

import java.sql.SQLException;
import java.util.List;

public interface ProductManager {
    void createProductTable() throws Exception;




    Product addProduct(String productName, String productType, int quantity) throws Exception;

    Product addProduct(String productName, String productType, int quantity, int dailyDeman, int reorderPoint) throws Exception;

    List<Product> readProducts(String productName, String productType);

    boolean removeProduct(int productId);

    void deleteProductsTable() throws SQLException;
}
