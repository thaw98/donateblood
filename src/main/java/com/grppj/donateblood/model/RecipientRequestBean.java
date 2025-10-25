package com.grppj.donateblood.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class RecipientRequestBean {
    private Integer requestId;
    private Integer quantity;
    private LocalDateTime requestDate;
    private LocalDate requiredDate;
    private String urgency;       // HIGH/MEDIUM/LOW
    private String status;        // pending/completed

    private Integer userId;
    private String username;
    private String email;
    private String phone;
    private String gender;
    private String dateOfBirth;
    private String address;

    private Integer bloodTypeId;
    private String bloodType;

    private Integer hospitalId;   // request's hospital
    private String hospitalName;  // request's hospital

    private Integer targetHospitalId;       
    private String targetHospitalName;      
    private String sourceHospitalName; 
    
    // NEW: computed in repository to drive button enable/disable in the UI
    private boolean canComplete;
}
