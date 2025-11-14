// src/main/java/com/grppj/donateblood/model/RequestDonorUsage.java
package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RequestDonorUsage {
    private final String donorName;
    private final String donorEmail;
    private final String bloodType;
    private final String donorPhone;
    private final String donorAddress;
    private final String fulfillmentTime; // formatted timestamp
}
