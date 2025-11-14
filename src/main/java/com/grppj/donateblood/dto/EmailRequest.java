package com.grppj.donateblood.dto;

public class EmailRequest {
    private String email;

    // Constructors
    public EmailRequest() {}

    public EmailRequest(String email) {
        this.email = email;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}