package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DonationBean {
    private Integer donationId;
    private Integer bloodUnit;
    private String donationDate;   // "yyyy-MM-dd HH:mm:ss"
    private String status;

    private Integer userId;
    private Integer userRoleId;
    private Integer hospitalId;

    private Integer donorAppointmentId;

    // NEW: appointment-linked info for the fragment
    private String appointmentDate; // e.g., "yyyy-MM-dd"
    private String hospitalName;
    private String bloodTypeName;
}

