package com.example.fff.model;


import com.example.fff.api.WarenausgangItemInterface;

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

