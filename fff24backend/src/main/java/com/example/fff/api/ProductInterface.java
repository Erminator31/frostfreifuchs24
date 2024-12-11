package com.example.fff.api;

public interface ProductInterface {

    int getDailyDemand();

    void setDailyDemand(int dailyDemand);

    int getReorderPoint();

    void setReorderPoint(int reorderPoint);

    int getReorderQuantity();

    void setReorderQuantity(int reorderQuantity);

    int getProductId();
    String getProductName();
    String getProductType();

    int getProductQuantity();

    void setProductQuantity(int quantity);
    void setProductName(String productName);

    void setProductType(String productType);
}
