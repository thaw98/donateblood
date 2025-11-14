package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class BloodStockBean {
    private Integer stockId;
    private Integer amount;
    private java.sql.Date updatedDate;
    private Integer hospitalId;
    private Integer bloodTypeId;
    private Integer userId;       // last updater
    private Integer userRoleId;   // last updater role
}
