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

    @Autowired
    private DonorAppointmentRepository donorAppointmentRepository;
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
        // donor.setDonateAgain(null); // removed: field no longer exists
        model.addAttribute("hospital", hospital);
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        // Block picking date under 18 y/o
        model.addAttribute("maxDob", LocalDate.now().minusYears(18).format(DateTimeFormatter.ISO_DATE));
        return "add-donor-admin";
    }

    // Handle donor submission + create a PENDING appointment (no donation yet)
    @PostMapping("/donors/add")
    public String addDonor(
            @Valid @ModelAttribute("donor") UserBean donor,
            BindingResult bindingResult,
            Model model,
            // from registration form
            @RequestParam("appointmentDate") String appointmentDate, // yyyy-MM-dd
            @RequestParam("appointmentTime") String appointmentTime, // HH:mm
            @RequestParam(value = "hospitalId", required = false) Integer hospitalId) {

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

        // Error handling - Phone
        String phone = donor.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "phone", "Phone is required."));
        } else if (!phone.trim().matches("\\d{9,15}")) {
            bindingResult.addError(new FieldError("donor", "phone", "Phone must be 9–15 digits."));
        }

        // Error handling - Gender
        String gender = donor.getGender();
        if (gender == null || gender.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "gender", "Gender is required."));
        }

        // Error handling - DOB, Must be over 18, Validation real date format
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

        // Error handling - Address
        String address = donor.getAddress();
        if (address == null || address.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "address", "Address is required."));
        }

        // (No donateAgain validation — removed feature)

        // If any errors, re-render the form (no redirect)
        if (bindingResult.hasErrors()) {
            model.addAttribute("hospital", hospitalRepository.getAllHospitals().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
            return "add-donor-admin";
        }

        donor.setRoleId(2); // Set to actual Donor role id

        // Insert new user and get new user id
        int userId = donorRepository.addDonor(donor);

        // ✅ Create a PENDING appointment tied to this new donor
        if (hospitalId == null) {
            hospitalId = hospitalRepository.getAllHospitals().get(0).getId(); // fallback
        }

        DonorAppointmentBean appt = new DonorAppointmentBean();
        appt.setUserId(userId);
        appt.setHospitalId(hospitalId);
        appt.setBloodTypeId(donor.getBloodTypeId());
        appt.setDate(appointmentDate);  // "yyyy-MM-dd"
        appt.setTime(appointmentTime);  // "HH:mm"
        appt.setStatus("pending");
        donorAppointmentRepository.createAppointment(appt);

        // Do NOT create a donation here — donation happens after approval

        return "redirect:/admin/appointments";
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
        // 1) Update basic fields (e.g., phone)
        donorRepository.updateDonor(donor); // now only updates phone per change above

        // 2) If admin set status to Used, flip the latest donation (if it was Available)
        if ("Used".equalsIgnoreCase(String.valueOf(donor.getStatus()))) {
            // find latest donation
            var latest = donorRepository.findLatestDonation(donor.getId(), 2 /* donor role_id */);
            if (latest != null) {
                int donationId = ((Number) latest.get("donation_id")).intValue();
                int changed = donorRepository.markDonationUsed(donationId);

                if (changed > 0) { // we actually transitioned Available -> Used
                    int hospitalId = ((Number) latest.get("hospital_id")).intValue();
                    int units      = ((Number) latest.get("blood_unit")).intValue();

                    // need donor's blood type (not posted by the form), fetch it
                    UserBean full = donorRepository.getDonorById(donor.getId());
                    Integer bloodTypeId = full.getBloodTypeId();

                    int adminId = 1, adminRoleId = 1; // TODO: use logged-in admin
                    bloodStockRepository.decreaseStock(hospitalId, bloodTypeId, units, adminId, adminRoleId);
                }
            }
        }

        return "redirect:/admin/donors";
    }

}
