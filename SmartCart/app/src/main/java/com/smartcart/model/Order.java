package com.smartcart.model;

public class Order {
    public long id, userId, placedAt, shippedAt, deliveredAt;
    public String code, status, address, payment;
    public double total;
}
