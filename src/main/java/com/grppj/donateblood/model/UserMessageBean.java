package com.grppj.donateblood.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserMessageBean {
    private int id;
    private int senderId;
    private int receiverId;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
    private String senderName;
    private LocalDate appointmentDate;
    private String appointmentTime;


 
}

