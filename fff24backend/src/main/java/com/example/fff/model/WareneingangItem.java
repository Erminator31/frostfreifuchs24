package com.example.fff.model;

import com.example.fff.api.WareneingangItemInterface;

/**
 * The WareneingangItem class represents an individual item in the goods receipt process
 * within an inventory management system. It encapsulates the product's unique identifier
 * and the quantity received.
 *
 * This class implements the WareneingangItemInterface, ensuring standardized methods
 * to access and modify the product ID and quantity attributes.
 */
public class WareneingangItem implements WareneingangItemInterface {
    private int productId;
    private int quantity;

    public WareneingangItem() {}

    public WareneingangItem(int productId, int quantity) {
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
