package com.example.fff.api;

import model.OrderItem;

import java.util.List;

public interface OrderInterface {
    int getOrderId();

    String getCustomerName();

    String getOrderDate();

    List<OrderItem> getItems();
}
