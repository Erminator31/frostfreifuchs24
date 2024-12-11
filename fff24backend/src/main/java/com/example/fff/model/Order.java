package com.example.fff.model;

import com.example.fff.api.OrderInterface;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Order implements OrderInterface {
    private int orderId;
    private String customerName;
    private String orderDate;
    private List<OrderItem> items;

    public Order(int orderId, String customerName, String orderDate, List<OrderItem> items) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.orderDate = orderDate;
        this.items = items;
    }

    @Override
    public int getOrderId() {
        return orderId;
    }

    @Override
    public String getCustomerName() {
        return customerName;
    }

    @Override
    public String getOrderDate() {
        return orderDate;
    }

    @Override
    public List<OrderItem> getItems() {
        return items;
    }



}
