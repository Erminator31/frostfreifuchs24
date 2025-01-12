package com.example.fff.api;

import com.example.fff.model.ForecastWeights;
import com.example.fff.model.Product;

import java.sql.SQLException;
import java.util.List;

public interface ProductManager {
    void createProductTable() throws Exception;


    Product readProductById(int productId) throws SQLException;

    Product addProduct(String productName, String productType, int quantity) throws Exception;

    Product addProduct(String productName, String productType, int quantity, int dailyDeman, int reorderPoint) throws Exception;

    List<Product> readProducts(String productName, String productType);

    boolean removeProduct(int productId);

    void deleteProductsTable() throws SQLException;

    void updateDailyDemand() throws Exception;

    void updateProductForecastValues(Product product,
                                     int newDailyDemand,
                                     int newReorderPoint,
                                     int newReorderQuantity) throws SQLException;

    ForecastWeights getForecastWeights() throws SQLException;

    void updateForecastWeights(double alpha, double beta, double gamma) throws SQLException;

    void createForecastWeightsTable() throws SQLException;
}
