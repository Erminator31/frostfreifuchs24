package com.example.fff.api;

import model.Order;
import model.OrderItem;

import java.util.List;

public interface OrderManager {
    Order createOrder(String customerName, List<OrderItem> items) throws Exception;
    Order getOrder(int orderId) throws Exception;
    List<Order> getAllOrders() throws Exception;
}

