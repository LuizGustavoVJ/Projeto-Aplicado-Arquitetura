package com.pip.dto;

/**
 * DTO para dados do cliente
 */
public class Customer {
    
    private String name;
    private String email;
    private String document;
    private String phone;
    
    public Customer() {
    }
    
    public Customer(String name, String email, String document) {
        this.name = name;
        this.email = email;
        this.document = document;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDocument() {
        return document;
    }
    
    public void setDocument(String document) {
        this.document = document;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
}
