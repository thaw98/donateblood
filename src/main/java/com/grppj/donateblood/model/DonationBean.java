package com.grppj.donateblood.model;

import java.time.LocalDateTime;

public class DonationBean {
    private Integer donationId;
    private Integer bloodUnit;
    private LocalDateTime donationDate;
    private String status;
    private Integer userId;
    private Integer userRoleId;
    private Integer hospitalId;

    // Constructors
    public DonationBean() {}

    public DonationBean(Integer donationId, Integer bloodUnit, LocalDateTime donationDate, 
                       String status, Integer userId, Integer userRoleId, Integer hospitalId) {
        this.donationId = donationId;
        this.bloodUnit = bloodUnit;
        this.donationDate = donationDate;
        this.status = status;
        this.userId = userId;
        this.userRoleId = userRoleId;
        this.hospitalId = hospitalId;
    }

    // Getters and Setters
    public Integer getDonationId() { return donationId; }
    public void setDonationId(Integer donationId) { this.donationId = donationId; }
    
    public Integer getBloodUnit() { return bloodUnit; }
    public void setBloodUnit(Integer bloodUnit) { this.bloodUnit = bloodUnit; }
    
    public LocalDateTime getDonationDate() { return donationDate; }
    public void setDonationDate(LocalDateTime donationDate) { this.donationDate = donationDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public Integer getUserRoleId() { return userRoleId; }
    public void setUserRoleId(Integer userRoleId) { this.userRoleId = userRoleId; }
    
    public Integer getHospitalId() { return hospitalId; }
    public void setHospitalId(Integer hospitalId) { this.hospitalId = hospitalId; }
}