package com.example.fff.api;

import model.Order;
import model.OrderItem;

import java.sql.SQLException;
import java.util.List;

public interface OrderManager {


    void createOrderTable() throws Exception;

    void createOrderItemTable() throws Exception;

    Order createOrder(String customerName, List<OrderItem> items) throws Exception;
    Order getOrder(int orderId) throws Exception;
    List<Order> getAllOrders() throws Exception;



    void deleteOrderTable() throws SQLException;

    void deleteOrderItemsTable() throws SQLException;
}

