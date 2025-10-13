package com.grppj.donateblood.controller;

import java.time.LocalDate;
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

import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.UserBean;
import com.grppj.donateblood.repository.BloodStockRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class DonorController {

    /** Your DB uses role_id = 3 for donors. */
    private static final int DONOR_ROLE = 3;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodTypeRepository bloodTypeRepository;

    @Autowired
    private BloodStockRepository bloodStockRepository;

    private static final Map<Integer, String> bloodTypeMap = new HashMap<>();
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
        var hospital = hospitalRepository.getAllHospitals().get(0); // choose current hospital
        UserBean donor = new UserBean();
        donor.setPassword("default123"); // default for convenience in dev

        model.addAttribute("hospital", hospital);
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        // Block picking DOB under 18 y/o
        model.addAttribute("maxDob",
                LocalDate.now().minusYears(18).format(DateTimeFormatter.ISO_DATE));
        return "admin/add-donor-admin";
    }

    // Handle donor submission and create an initial "Available" donation row
    @PostMapping("/donors/add")
    public String addDonor(
            @Valid @ModelAttribute("donor") UserBean donor,
            BindingResult bindingResult,
            Model model) {

        // ----- Username -----
        String username = donor.getUsername();
        if (username == null || username.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "username", "Username is required."));
        }

        // ----- Email -----
        String rawEmail = donor.getEmail();
        String email = (rawEmail == null) ? "" : rawEmail.trim();
        if (email.isEmpty()) {
            bindingResult.addError(new FieldError("donor", "email", email, false, null, null, "Email is required."));
        } else if (!EMAIL_RE.matcher(email).matches()) {
            bindingResult.addError(new FieldError("donor", "email", email, false, null, null, "Please enter a valid email address."));
        } else if (donorRepository.emailExists(email)) {
            bindingResult.addError(new FieldError("donor", "email", email, false, null, null, "This email is already registered."));
        }

        // ----- Phone -----
        String phone = donor.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "phone", "Phone is required."));
        } else if (!phone.trim().matches("\\d{9,15}")) {
            bindingResult.addError(new FieldError("donor", "phone", "Phone must be 9–15 digits."));
        }

        // ----- Gender -----
        String gender = donor.getGender();
        if (gender == null || gender.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "gender", "Gender is required."));
        }

        // ----- Date of Birth (18+) -----
        String dobStr = donor.getDateOfBirth();
        if (dobStr == null || dobStr.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "dateOfBirth", "Date of birth is required."));
        } else {
            try {
                var dob = LocalDate.parse(dobStr, DateTimeFormatter.ISO_DATE);
                int age = java.time.Period.between(dob, LocalDate.now()).getYears();
                if (age < 18) {
                    bindingResult.addError(new FieldError("donor", "dateOfBirth", "Donor must be at least 18 years old."));
                }
            } catch (java.time.format.DateTimeParseException ex) {
                bindingResult.addError(new FieldError("donor", "dateOfBirth", "Invalid date format (yyyy-MM-dd)."));
            }
        }

        // ----- Blood Type -----
        Integer bloodTypeId = donor.getBloodTypeId();
        if (bloodTypeId == null) {
            bindingResult.addError(new FieldError("donor", "bloodTypeId", "Blood type is required."));
        }

        // ----- Address -----
        String address = donor.getAddress();
        if (address == null || address.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "address", "Address is required."));
        }

        // If any validation errors, return to form
        if (bindingResult.hasErrors()) {
            model.addAttribute("hospital", hospitalRepository.getAllHospitals().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
            return "admin/add-donor-admin";
        }

        // ---------- Persist user ----------
        donor.setEmail(email);                 // store the trimmed value
        donor.setRoleId(DONOR_ROLE);           // role_id 3 for donors
        if (donor.getPassword() == null || donor.getPassword().isBlank()) {
            donor.setPassword("default123");
        }
        int userId = donorRepository.addDonor(donor);

        // READ BACK actual role to satisfy (user_id, user_role_id) composite FK
        Integer actualRoleId = donorRepository.findUserRoleId(userId);
        if (actualRoleId == null) actualRoleId = DONOR_ROLE;

        // ---------- Seed an initial "Available" donation ----------
        int hospitalId = hospitalRepository.getAllHospitals().get(0).getId();

        DonationBean donation = new DonationBean();
        donation.setBloodUnit(1);
        donation.setDonationDate(
            java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        donation.setStatus("Available");
        donation.setUserId(userId);
        donation.setUserRoleId(actualRoleId);   // MUST match user.role_id to pass FK
        donation.setHospitalId(hospitalId);

        donorRepository.addDonation(donation);

        return "redirect:/admin/donors";
    }

    @GetMapping("/donors")
    public String showDonorList(Model model) {
        final int DONOR_ROLE = 3; // your DB’s donor role
        List<UserBean> donorList = donorRepository.getAllDonorsWithStatus(DONOR_ROLE);
        model.addAttribute("donorList", donorList);
        model.addAttribute("bloodTypeMap", bloodTypeMap);
        return "admin/donor-list";
    }


    @GetMapping("/donors/edit/{id}")
    public String showEditDonorForm(@PathVariable("id") int id, Model model) {
        UserBean donor = donorRepository.getDonorById(id);
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypeMap", bloodTypeMap);
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        model.addAttribute("hospitals", hospitalRepository.getAllHospitals());
        return "admin/edit-donor-admin";
    }

    @PostMapping("/donors/update")
    public String updateDonor(@ModelAttribute("donor") UserBean donor) {
        // update basic fields (phone, etc.)
        donorRepository.updateDonor(donor);

        // If admin selected "Used", consume the latest Available donation once
        if ("Used".equalsIgnoreCase(String.valueOf(donor.getStatus()))) {
            var latest = donorRepository.findLatestDonation(donor.getId(), DONOR_ROLE); // donor role = 3
            if (latest != null) {
                int donationId = ((Number) latest.get("donation_id")).intValue();
                int changed = donorRepository.markDonationUsed(donationId); // only if currently Available
                if (changed > 0) {
                    int hospitalId  = ((Number) latest.get("hospital_id")).intValue();
                    int units       = ((Number) latest.get("blood_unit")).intValue();
                    Integer btId    = donorRepository.getDonorById(donor.getId()).getBloodTypeId();
                    bloodStockRepository.decreaseStock(hospitalId, btId, units, 1, 1); // adminId/adminRoleId
                }
            }
        }

        return "redirect:/admin/donors";
    }
}
