package model;


import com.example.fff.api.OrderInterface;
import com.example.fff.api.OrderItemInterface;

public class OrderItem implements OrderItemInterface{
    private int productId;
    private int quantity;

    public OrderItem() {}

    public OrderItem(int productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
@Override
    public int getProductId() {
        return productId;
    }
@Override
    public int getQuantity() {
        return quantity;
    }
}

