package com.grppj.donateblood.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BloodRequest {

	private Integer id;
    private Integer quantity;
    
    private LocalDate requiredDate;
    private Urgency urgency; // enum
    private AppointmentStatus status;
    private Integer userId;
    private Integer hospitalId;
    private Integer bloodTypeId;
    
    // Extra fields for display
    private String hospitalName;
    private String bloodTypeName;

}

