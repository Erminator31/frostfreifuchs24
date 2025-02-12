package com.example.fff.api;

/**
 * The WarenausgangItemInterface defines the structure for managing individual items
 * in the shipping process within an inventory or warehouse system. Implementations
 * of this interface should provide mechanisms for accessing and modifying attributes
 * such as the product identifier and quantity associated with the item.
 */
public interface WarenausgangItemInterface {
    int getProductId();

    void setProductId(int productId);

    int getQuantity();

    void setQuantity(int quantity);
}
