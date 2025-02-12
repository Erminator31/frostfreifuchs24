package com.example.fff.model;


import com.example.fff.api.WarenausgangItemInterface;

/**
 * The WarenausgangItem class represents an individual item in a shipping process.
 * It includes details such as the product identifier and the quantity of the product
 * being shipped.
 *
 * This class implements the WarenausgangItemInterface to ensure a standard structure
 * for managing individual items in the shipping process within an inventory or warehouse system.
 */
public class WarenausgangItem implements WarenausgangItemInterface {
    private int productId;
    private int quantity;

    public WarenausgangItem() {}

    public WarenausgangItem(int productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public int getProductId() { return productId; }
    @Override
    public void setProductId(int productId) { this.productId = productId; }
    @Override
    public int getQuantity() { return quantity; }
    @Override
    public void setQuantity(int quantity) { this.quantity = quantity; }
}

