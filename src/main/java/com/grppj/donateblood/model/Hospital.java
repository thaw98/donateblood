package com.grppj.donateblood.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Hospital {
    private int id;
    private String hospitalName;
    private String address;
    private String phoneNo;
    private String gmail;
    
    private List<String> profilePictures;
    private Long createdBy;

}
