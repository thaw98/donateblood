// src/main/java/com/grppj/donateblood/model/FulfillmentSummary.java
package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FulfillmentSummary {
    private final Integer requestId;

    // recipient info
    private final String  recipientName;
    private final String  recipientEmail;
    private final String  recipientPhone;
    private final String  recipientAddress;

    // blood / usage
    private final String  bloodType;
    private final Integer totalUsed;     // SUM(rf.quantity_used)

    // most recent completion datetime (formatted in SQL)
    private final String  completedAt;   // alias: completed_at

    // optional (may be null for some queries)
    private final String  donationIdsCsv;

    private final String  requestStatus; // br.status
}
