package com.grppj.donateblood.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {
    private Integer id;
    private LocalDate date;
    private String time;
    private LocalDateTime createdAt; // Changed to LocalDateTime
    private AppointmentStatus status;
    private Integer userId;
    private Integer hospitalId;
    private Integer bloodTypeId;

    // Extra fields for display
    private String hospitalName;
    private String bloodTypeName;
    private Integer userRoleId;
}