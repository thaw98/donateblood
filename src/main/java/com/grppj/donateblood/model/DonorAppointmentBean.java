package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DonorAppointmentBean {
    private Integer id;
    private String date;
    private String time;
    private String createdAt;
    private String status;
    private Integer userId;
    private Integer adminId;
    private Integer hospitalId;
    private Integer bloodTypeId;
}
