package com.grppj.donateblood.model;

import java.util.List;
import java.util.Arrays;
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

    // Store filenames as a single comma-separated string
    private String profilePicture;  // matches varchar column in DB

    private int adminId;

    // ===== Helper to convert to list =====
    public List<String> getProfilePictures() {
        if (profilePicture == null || profilePicture.isEmpty()) return List.of();
        return Arrays.asList(profilePicture.split(","));
    }

    // ===== Validation =====
    public boolean isValidProfilePictures() {
        return getProfilePictures().size() <= 5;
    }

    public String getProfilePicturesValidationMessage() {
        int count = getProfilePictures().size();
        if (count == 0) return "No images selected";
        if (count > 5) return "Maximum 5 images allowed. You selected: " + count;
        return "Valid: " + count + " images selected";
    }
}
