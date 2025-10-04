package com.grppj.donateblood.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.grppj.donateblood.model.BloodTypeBean;
import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.HospitalBean;
import com.grppj.donateblood.model.UserBean;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;

@Controller
@RequestMapping("/admin")
public class DonorController {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodTypeRepository bloodTypeRepository;

    // Show add donor form
    @GetMapping("/donors/add")
    public String showAddDonorForm(Model model) {
        HospitalBean hospital = hospitalRepository.getAllHospitals().get(0); // Use your hospital logic
        UserBean donor = new UserBean();
        donor.setDonateAgain(null); // <--- THIS LINE ensures no radio preselected
        model.addAttribute("hospital", hospital);
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        return "add-donor-admin";
    }

    // Handle donor submission and display all donors
    @PostMapping("/donors/add")
    public String addDonor(@ModelAttribute("donor") UserBean donor, Model model) {
        donor.setRoleId(2); // Set to actual Donor role id

        // 1. Insert new user and get new user id
        int userId = donorRepository.addDonor(donor);

        // 2. Prepare and insert DonationBean
        DonationBean donation = new DonationBean();
        donation.setBloodUnit(1); // set from your logic or form
        donation.setDonationDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        donation.setStatus("Available");
        donation.setUserId(userId);
        donation.setUserRoleId(donor.getRoleId());
        donation.setHospitalId(1); // use your actual hospital selection

        donorRepository.addDonation(donation);

        // 3. Get blood type name for output
        BloodTypeBean bloodType = bloodTypeRepository.getBloodTypeById(donor.getBloodTypeId());

        // 4. Get all donors to display as a list/table
        List<UserBean> donorList = donorRepository.getAllDonors();

        HospitalBean hospital = hospitalRepository.getAllHospitals().get(0);

        model.addAttribute("submissionDateTime", donation.getDonationDate());
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypeName", bloodType.getBloodType());
        model.addAttribute("hospital", hospital);
        model.addAttribute("donorList", donorList);

        return "redirect:/admin/donors";
    }
    @GetMapping("/donors")
    public String showDonorList(Model model) {
        List<UserBean> donorList = donorRepository.getAllDonorsWithStatus();
        model.addAttribute("donorList", donorList);
        return "donor-list"; // This should be your new Thymeleaf template
    }
    
    
}