package com.grppj.donateblood.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import com.grppj.donateblood.model.UserBean;
import com.grppj.donateblood.model.HospitalBean;
import com.grppj.donateblood.model.BloodTypeBean;
import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.repository.UserRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonationRepository;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class DonorController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodTypeRepository bloodTypeRepository;

    @Autowired
    private DonationRepository donationRepository;

    @GetMapping("/donors/add")
    public String showAddDonorForm(Model model) {
        // Get default hospital (first hospital in the database)
        HospitalBean hospital = hospitalRepository.getDefaultHospital();
        
        // Get all blood types for dropdown
        List<BloodTypeBean> bloodTypes = bloodTypeRepository.getAllBloodTypes();
        
        model.addAttribute("donor", new UserBean());
        model.addAttribute("hospital", hospital);
        model.addAttribute("bloodTypes", bloodTypes);
        
        return "add-donor-admin";
    }

    @PostMapping("/donors/add")
    public String addDonor(@ModelAttribute UserBean donor, Model model) {
        try {
            // Set default password (you might want to generate a random one)
            donor.setPassword("defaultPassword123");
            
            // Get donor role ID
            Integer donorRoleId = userRepository.getDonorRoleId();
            donor.setRoleId(donorRoleId);
            
            // Set username as email (or you can generate one)
            donor.setUsername(donor.getEmail());
            
            // Save user to database
            int userResult = userRepository.saveUser(donor);
            
            if (userResult > 0) {
                // Get the newly created user to get the generated ID
                UserBean savedUser = userRepository.getUserByEmail(donor.getEmail());
                
                // Get default hospital for donation record
                HospitalBean hospital = hospitalRepository.getDefaultHospital();
                
                // Create donation record with status 'Used'
                DonationBean donation = new DonationBean();
                donation.setUserId(savedUser.getId());
                donation.setUserRoleId(donorRoleId);
                donation.setHospitalId(hospital.getId());
                donation.setBloodUnit(1); // Default 1 unit
                
                // Save donation record
                int donationResult = donationRepository.saveDonation(donation);
                
                if (donationResult > 0) {
                    // Get the submission date and time
                    LocalDateTime submissionDateTime = LocalDateTime.now();
                    
                    // Add data to model for display
                    model.addAttribute("donor", savedUser);
                    model.addAttribute("hospital", hospital);
                    model.addAttribute("submissionDateTime", submissionDateTime);
                    model.addAttribute("donation", donationRepository.getDonationById(donation.getDonationId()));
                    
                    return "donor-add-success";
                }
            }
            
            model.addAttribute("error", "Failed to add donor. Please try again.");
            return "add-donor-admin";
            
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred: " + e.getMessage());
            return "add-donor-admin";
        }
    }
}