package com.grppj.donateblood.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class UserBean {
    private Integer id;
    private String username;
    private String email;
    private String phone;
    private String password;
    private String gender;
    private String dateOfBirth;
    private String address;
    private Integer bloodTypeId;
    private Integer roleId;
    private String status;
    private String hospitalName;
    private Integer donateAgain;

    
    // Getters and setters
    // ...
    
}
