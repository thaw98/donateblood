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
import org.springframework.web.bind.annotation.RequestParam;

import com.grppj.donateblood.model.BloodTypeBean;
import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.DonorAppointmentBean;
import com.grppj.donateblood.model.HospitalBean;
import com.grppj.donateblood.model.UserBean;
import com.grppj.donateblood.repository.BloodStockRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonorAppointmentRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.repository.HospitalsRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class DonorController {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private HospitalsRepository hospitalRepository;

    @Autowired
    private BloodTypeRepository bloodTypeRepository;

    @Autowired 
    private BloodStockRepository bloodStockRepository;


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
        donor.setPassword("default123"); // <-- ADD THIS LINE
        // donor.setDonateAgain(null); // removed: field no longer exists
        model.addAttribute("hospital", hospital);
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        // Block picking date under 18 y/o
        model.addAttribute("maxDob", LocalDate.now().minusYears(18).format(DateTimeFormatter.ISO_DATE));
        return "admin/add-donor-admin";
    }

    // Handle donor submission + create a PENDING appointment (no donation yet)
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

        if (bindingResult.hasErrors()) {
            model.addAttribute("hospital", hospitalRepository.getAllHospitals().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
            return "admin/add-donor-admin";
        }

        donor.setRoleId(2);                           // donor role
 	   // ensure a default if empty
	     if (donor.getPassword() == null || donor.getPassword().isBlank()) {
	         donor.setPassword("default123");          // <-- ADD THIS
	     }
        int userId = donorRepository.addDonor(donor); // insert user

        // Immediately create a donation so the row shows "Available" and "Next Eligible"
        int hospitalId = hospitalRepository.getAllHospitals().get(0).getId();

        DonationBean donation = new DonationBean();
        donation.setBloodUnit(1);
        donation.setDonationDate(
            java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        donation.setStatus("Available");
        donation.setUserId(userId);
        donation.setUserRoleId(donor.getRoleId());
        donation.setHospitalId(hospitalId);

        donorRepository.addDonation(donation);

        // keep trimmed/validated value
        donor.setEmail(email);

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
                java.time.LocalDate dob = java.time.LocalDate.parse(dobStr, java.time.format.DateTimeFormatter.ISO_DATE);
                int age = java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
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

        // Re-render form if any errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("hospital", hospitalRepository.getAllHospitals().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
            return "admin/add-donor-admin";
        }

        donor.setRoleId(2);            // donor role

        // No appointment created; go back to donors list
        return "redirect:/admin/donors";
    }

    @GetMapping("/donors")
    public String showDonorList(Model model) {
        List<UserBean> donorList = donorRepository.getAllDonorsWithStatus();
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
        // update basic fields (phone)
        donorRepository.updateDonor(donor);

        // If admin selected "Used", consume the latest Available donation once
        if ("Used".equalsIgnoreCase(String.valueOf(donor.getStatus()))) {
            var latest = donorRepository.findLatestDonation(donor.getId(), 2); // role_id 2 = donor
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
