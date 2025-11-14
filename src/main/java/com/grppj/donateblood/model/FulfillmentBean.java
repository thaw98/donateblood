// src/main/java/com/grppj/donateblood/model/FulfillmentBean.java
package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FulfillmentBean {
    private final Integer fulfillmentId;
    private final String  fulfillmentDate;  // e.g. "2025-10-23 10:30PM"
    private final Integer quantityUsed;
    private final Integer donationId;
    private final Integer requestId;

    private final String  bloodType;        // from blood_request
    private final String  recipientName;    // u_req.username
    private final String  donorName;        // u_donor.username (optional / left join)
    private final String  requestStatus;    // br.status
}
