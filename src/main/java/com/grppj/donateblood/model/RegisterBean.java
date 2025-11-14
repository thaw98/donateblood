package com.grppj.donateblood.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterBean {

 private int id;
 private String username;
 private String email;
 private String password;
 private String gender;
 private LocalDate dob;
 private String Address;
 private RoleBean role;

}