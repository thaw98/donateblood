package com.grppj.donateblood.model;

public class HospitalBean {
    private Integer id;
    private String hospitalName;
    private String address;
    private String contact;

    // Constructors
    public HospitalBean() {}

    public HospitalBean(Integer id, String hospitalName, String address, String contact) {
        this.id = id;
        this.hospitalName = hospitalName;
        this.address = address;
        this.contact = contact;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
}