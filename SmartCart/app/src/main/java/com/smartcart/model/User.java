package com.smartcart.model;

public class User {
    public long id;
    public String name, email, role, tier;
    public boolean isAdmin() { return "admin".equals(role); }
}
