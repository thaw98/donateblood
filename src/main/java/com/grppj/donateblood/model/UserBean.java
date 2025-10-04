package com.grppj.donateblood.model;

import java.time.LocalDateTime;

public class UserBean {
    private Integer id;
    private String username;
    private String email;
    private String password;
    private String gender;
    private String dateofbirth;
    private String address;
    private Integer roleId;
    private LocalDateTime createdAt;

    // Constructors
    public UserBean() {}

    public UserBean(Integer id, String username, String email, String password, 
                   String gender, String dateofbirth, String address, 
                   Integer roleId, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.gender = gender;
        this.dateofbirth = dateofbirth;
        this.address = address;
        this.roleId = roleId;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getDateofbirth() { return dateofbirth; }
    public void setDateofbirth(String dateofbirth) { this.dateofbirth = dateofbirth; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}