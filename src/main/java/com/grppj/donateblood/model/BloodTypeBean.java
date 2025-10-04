package com.grppj.donateblood.model;

public class BloodTypeBean {
    private Integer id;
    private String bloodType;

    // Constructors
    public BloodTypeBean() {}

    public BloodTypeBean(Integer id, String bloodType) {
        this.id = id;
        this.bloodType = bloodType;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
}