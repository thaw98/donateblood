package com.grppj.donateblood.dto;

public class CodeLoginRequest {
    private String email;
    private String verificationCode;

    // Constructors
    public CodeLoginRequest() {}

    public CodeLoginRequest(String email, String verificationCode) {
        this.email = email;
        this.verificationCode = verificationCode;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}