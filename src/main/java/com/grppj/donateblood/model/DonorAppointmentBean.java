package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DonorAppointmentBean {
    private Integer id;
    private String date;        // yyyy-MM-dd from form
    private String time;        // HH:mm from form
    private String createdAt;   // set by DB or app
    private AppointmentStatus status;
    private Integer userId;
    private Integer adminId;    // who approved/rejected (nullable)
    private Integer hospitalId;
    private Integer bloodTypeId;
}
