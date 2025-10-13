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

    // kept if you still use them elsewhere (e.g., computing next valid date)
    private Integer userId;
    private Integer userRoleId;
    private Integer hospitalId;

    // NEW: tie donation -> donor_appointment
    private Integer donorAppointmentId;
}
