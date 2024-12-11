package com.example.fff.api;

public interface ProductInterface {

    int getProductId();
    String getProductName();
    String getProductType();

    int getProductQuantity();

    void setProductQuantity(int quantity);
    void setProductName(String productName);

    void setProductType(String productType);
}
