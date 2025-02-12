package com.example.fff.api;

/**
 * The WareneingangItemInterface represents the structure for handling individual items
 * in the goods receipt process within an inventory management system. Implementations
 * of this interface should provide methods to access and modify the product attributes
 * such as the product identifier and its quantity associated with the receipt record.
 */
public interface WareneingangItemInterface {
    int getProductId();

    void setProductId(int productId);

    int getQuantity();

    void setQuantity(int quantity);
}
