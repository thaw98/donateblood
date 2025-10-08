package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DonationBean {
    private Integer donationId;
    private Integer bloodUnit;
    private String donationDate;
    private String status;
    private Integer userId;
    private Integer userRoleId;
    private Integer hospitalId;
}
