package com.example.fff.api;

/**
 * The ProductInterface provides the methods to manage product-related attributes and behaviors
 * in an inventory or product management system. Implementing classes should define how the
 * product's details such as quantity, demand, and reorder information are retrieved and updated.
 */
public interface ProductInterface {

    int getDailyDemand();

    void setDailyDemand(int dailyDemand);

    int getReorderPoint();

    void setReorderPoint(int reorderPoint);

    void setReorderQuantity(int reorderQuantity);

    int getReorderQuantity();


    int getProductId();
    String getProductName();
    String getProductType();

    int getProductQuantity();

    void setProductQuantity(int quantity);
    void setProductName(String productName);

    void setProductType(String productType);
}
