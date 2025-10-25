package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class HospitalBean {
    private Integer id;
    private String hospitalName;
    private String address;
    private String phone;
}
