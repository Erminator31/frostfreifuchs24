package com.example.fff.api;

import model.Product;

import java.util.List;

public interface ProductManager {
    void createProductTable() throws Exception;

    Product addProduct(String productName, String productType);
    List<Product> readProducts(String productName, String productType);

}
