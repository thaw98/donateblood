package com.grppj.donateblood.model;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BloodRequestBean {
    private Integer id;
    private Integer quantity;            // units requested
    private String  requestDate;         // auto NOW() from DB, but you can read back
    private String  requiredDate;        // yyyy-MM-dd
    private String  urgency;             // High / Medium / Low
    private String  status;              // pending / fulfilled
    private Integer userId;              // requester user
    private Integer userRoleId;          // requester role (e.g. 3=recipient)
    private Integer hospitalId;
    private Integer bloodTypeId;
}
