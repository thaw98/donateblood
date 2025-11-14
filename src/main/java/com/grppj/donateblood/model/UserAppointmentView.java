package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAppointmentView {
    private Integer userId;
    private String username;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateofbirth;
    private String address;
    private String userType;
    private String bloodType;
    private String hospitalName;
    private Integer appointmentId;
    private LocalDate appointmentDate;
    private String appointmentStatus;
    private byte[] imageBytes;
    
    // Helper method to get age from date of birth
    public String getAge() {
        if (dateofbirth != null) {
            return String.valueOf(java.time.Period.between(dateofbirth, java.time.LocalDate.now()).getYears());
        }
        return "N/A";
    }
}