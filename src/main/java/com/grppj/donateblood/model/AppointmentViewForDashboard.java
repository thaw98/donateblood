package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentViewForDashboard {
    private Integer id;
    private String username;
    private String userEmail;
    private String userType;
    private String bloodType;
    private String appointmentDate;
    private String status;
    private Integer hospitalId;
    private String hospitalName;
}