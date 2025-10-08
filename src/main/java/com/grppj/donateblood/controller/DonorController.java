package com.grppj.donateblood.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.grppj.donateblood.model.BloodTypeBean;
import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.HospitalBean;
import com.grppj.donateblood.model.UserBean;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class DonorController {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodTypeRepository bloodTypeRepository;
    
    private static Map<Integer, String> bloodTypeMap = new HashMap<>();
    static {
        bloodTypeMap.put(1, "A+");
        bloodTypeMap.put(2, "A-");
        bloodTypeMap.put(3, "B+");
        bloodTypeMap.put(4, "B-");
        bloodTypeMap.put(5, "AB+");
        bloodTypeMap.put(6, "AB-");
        bloodTypeMap.put(7, "O+");
        bloodTypeMap.put(8, "O-");
    }
    
    private static final Pattern EMAIL_RE =
    	    Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    // Show add donor form
    @GetMapping("/donors/add")
    public String showAddDonorForm(Model model) {
        HospitalBean hospital = hospitalRepository.getAllHospitals().get(0); // Use your hospital logic
        UserBean donor = new UserBean();
        donor.setDonateAgain(null); // <--- THIS LINE ensures no radio preselected
        model.addAttribute("hospital", hospital);
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        //Block picking date under 18 y/o
        model.addAttribute("maxDob", LocalDate.now().minusYears(18).format(DateTimeFormatter.ISO_DATE));
        return "add-donor-admin";
    }

    // Handle donor submission and display all donors
    @PostMapping("/donors/add")
    public String addDonor(@Valid @ModelAttribute("donor") UserBean donor, BindingResult bindingResult, Model model) {
        // Error handling - Username
        String username = donor.getUsername();
        if (username == null || username.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "username", "Username is required."));
        }

        // Error handling - Email
        String rawEmail = donor.getEmail();
        String email = (rawEmail == null) ? "" : rawEmail.trim();

        if (email.isEmpty()) {
            bindingResult.addError(new FieldError(
                "donor", "email", email, false, null, null, "Email is required."));
        } else if (!EMAIL_RE.matcher(email).matches()) {
            bindingResult.addError(new FieldError(
                "donor", "email", email, false, null, null, "Please enter a valid email address."));
        } else if (donorRepository.emailExists(email)) {
            bindingResult.addError(new FieldError(
                "donor", "email", email, false, null, null, "This email is already registered."));
        }


        // If any error -> re-render the form with messages
        if (bindingResult.hasErrors()) {
            model.addAttribute("hospital", hospitalRepository.getAllHospitals().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
            return "add-donor-admin";
        }
        
        // add previous value back, if error occur
        donor.setEmail(email);
        //Error handling - Phone
        String phone = donor.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "phone", "Phone is required."));
        } else if (!phone.trim().matches("\\d{9,15}")) {
            bindingResult.addError(new FieldError("donor", "phone", "Phone must be 9–15 digits."));
        }
        //Error handling - Gender
        String gender = donor.getGender();
        if (gender == null || gender.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "gender", "Gender is required."));
        }
        //Error handling - DOB, Must be over 18, Validation real date format
        String dobStr = donor.getDateOfBirth();
        if (dobStr == null || dobStr.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "dateOfBirth", "Date of birth is required."));
        } else {
            try {
                java.time.LocalDate dob = java.time.LocalDate.parse(
                        dobStr, java.time.format.DateTimeFormatter.ISO_DATE);
                int age = java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
                if (age < 18) {
                    bindingResult.addError(new FieldError("donor", "dateOfBirth",
                            "Donor must be at least 18 years old."));
                }
            } catch (java.time.format.DateTimeParseException ex) {
                bindingResult.addError(new FieldError("donor", "dateOfBirth",
                        "Invalid date format (yyyy-MM-dd)."));
            }
        }
        // Error handling - Blood Type
        Integer bloodTypeId = donor.getBloodTypeId();
        if (bloodTypeId == null) {
            bindingResult.addError(new FieldError("donor", "bloodTypeId", "Blood type is required."));
        }
        //Error handling - Address
        String address = donor.getAddress();
        if (address == null || address.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "address", "Address is required."));
        }
        //Error handling - Donate Again
        Integer donateAgain = donor.getDonateAgain();
        if (donateAgain == null) {
            bindingResult.addError(new FieldError("donor", "donateAgain",
                    "Please choose whether you would like to donate or not."));
        }

        // If any errors, re-render the form (no redirect)
        if (bindingResult.hasErrors()) {
            model.addAttribute("hospital", hospitalRepository.getAllHospitals().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
            return "add-donor-admin";
        }

        donor.setRoleId(2); // Set to actual Donor role id

        // Insert new user and get new user id
        int userId = donorRepository.addDonor(donor);

        // Prepare and insert DonationBean
        DonationBean donation = new DonationBean();
        donation.setBloodUnit(1); // set from your logic or form
        donation.setDonationDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        donation.setStatus("Available");
        donation.setUserId(userId);
        donation.setUserRoleId(donor.getRoleId());
        donation.setHospitalId(1); // use your actual hospital selection (When superadmin part is done, must change this line)

        donorRepository.addDonation(donation);

        // Get blood type name for output
        BloodTypeBean bloodType = bloodTypeRepository.getBloodTypeById(donor.getBloodTypeId());

        // Get all donors to display as a list/table
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
        model.addAttribute("bloodTypeMap", bloodTypeMap);
        return "donor-list";
    }
    
    @GetMapping("/donors/edit/{id}")
    public String showEditDonorForm(@PathVariable("id") int id, Model model) {
        UserBean donor = donorRepository.getDonorById(id); 
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypeMap", bloodTypeMap);
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        model.addAttribute("hospitals", hospitalRepository.getAllHospitals());
        return "edit-donor-admin";  
    }
    
    @PostMapping("/donors/update")
    public String updateDonor(@ModelAttribute("donor") UserBean donor) {
        donorRepository.updateDonor(donor);
        return "redirect:/admin/donors";
    }
}