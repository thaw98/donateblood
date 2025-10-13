package com.grppj.donateblood.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonorAppointmentRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class DonorController {

    /** donors have role_id = 3 */
    private static final int DONOR_ROLE = 3;

    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BloodTypeRepository bloodTypeRepository;
    @Autowired private DonorAppointmentRepository apptRepo;

    /** Use direct JDBC here to update phone only (avoid mass donation status update). */
    @Autowired private JdbcTemplate jdbcTemplate;

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

    // ===== Add Donor (GET) =====
    @GetMapping("/donors/add")
    public String showAddDonorForm(Model model) {
        var hospital = hospitalRepository.getAllHospitals().get(0); // pick current hospital
        UserBean donor = new UserBean();
        model.addAttribute("hospital", hospital);
        model.addAttribute("donor", donor);
        model.addAttribute("active", "add-donor");
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        model.addAttribute("maxDob",
            LocalDate.now().minusYears(18).format(DateTimeFormatter.ISO_DATE));
        return "admin/add-donor-admin";
    }

    // ===== Add Donor (POST) =====
    @PostMapping("/donors/add")
    public String addDonor(@Valid @ModelAttribute("donor") UserBean donor,
                           BindingResult bindingResult,
                           Model model) {

        // ---- username
        if (donor.getUsername() == null || donor.getUsername().trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "username", "Username is required."));
        }

        // ---- email (required, format, unique)
        String email = donor.getEmail() == null ? "" : donor.getEmail().trim();
        if (email.isEmpty()) {
            bindingResult.addError(new FieldError("donor", "email", email, false, null, null, "Email is required."));
        } else if (!EMAIL_RE.matcher(email).matches()) {
            bindingResult.addError(new FieldError("donor", "email", email, false, null, null, "Please enter a valid email address."));
        } else if (donorRepository.emailExists(email)) {
            bindingResult.addError(new FieldError("donor", "email", email, false, null, null, "This email is already registered."));
        }

        // ---- phone
        String phone = donor.getPhone();
        if (phone == null || phone.trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "phone", "Phone is required."));
        } else if (!phone.trim().matches("\\d{9,15}")) {
            bindingResult.addError(new FieldError("donor", "phone", "Phone must be 9–15 digits."));
        }

        // ---- gender
        if (donor.getGender() == null || donor.getGender().trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "gender", "Gender is required."));
        }

        // ---- DOB 18+
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

        // ---- blood type
        if (donor.getBloodTypeId() == null) {
            bindingResult.addError(new FieldError("donor", "bloodTypeId", "Blood type is required."));
        }

        // ---- address
        if (donor.getAddress() == null || donor.getAddress().trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "address", "Address is required."));
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("hospital", hospitalRepository.getAllHospitals().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
            return "admin/add-donor-admin";
        }

        // 1) Create donor user
        donor.setRoleId(DONOR_ROLE);
        int userId = donorRepository.addDonor(donor);

        // 2) Resolve role/hospital
        int actualRoleId = donorRepository.findUserRoleId(userId);
        int hospitalId   = hospitalRepository.getAllHospitals().get(0).getId();

        // 3) Create an APPROVED appointment now (admin-created)
        int apptId = apptRepo.createCompletedAppointmentNow(
                userId, actualRoleId, hospitalId, donor.getBloodTypeId()
        );

        // 4) Create initial Available donation tied to that appointment (→ stock +1)
        DonationBean donation = new DonationBean();
        donation.setBloodUnit(1);
        donation.setDonationDate(
            java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        donation.setStatus("Available");
        donation.setDonorAppointmentId(apptId);
        donorRepository.addDonation(donation);

        return "redirect:/admin/donors";
    }

    // ===== List donors (those with approved/completed appointments; repo handles it) =====
    @GetMapping("/donors")
    public String showDonorList(Model model) {
        List<UserBean> donorList = donorRepository.getAllDonorsWithStatus(DONOR_ROLE);
        model.addAttribute("donorList", donorList);
        model.addAttribute("active", "donors");
        model.addAttribute("bloodTypeMap", bloodTypeMap);
        return "admin/donor-list";
    }

    // ===== Edit donor (GET) =====
    @GetMapping("/donors/edit/{id}")
    public String showEditDonorForm(@PathVariable("id") int id, Model model) {
        UserBean donor = donorRepository.getDonorById(id);
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypeMap", bloodTypeMap);
        model.addAttribute("bloodTypes", bloodTypeRepository.getAllBloodTypes());
        model.addAttribute("hospitals", hospitalRepository.getAllHospitals());
        return "admin/edit-donor-admin";
    }

    // ===== Edit donor (POST) =====
    @PostMapping("/donors/update")
    public String updateDonor(@ModelAttribute("donor") UserBean donor) {
        // Update phone ONLY (avoid repository's mass status update)
        jdbcTemplate.update("UPDATE `user` SET phone = ? WHERE id = ?", donor.getPhone(), donor.getId());

        // If admin selected "Used", consume exactly one latest Available donation
        if ("Used".equalsIgnoreCase(String.valueOf(donor.getStatus()))) {
            var latest = donorRepository.findLatestDonation(donor.getId(), DONOR_ROLE);
            if (latest != null) {
                Integer donationId = (Integer) latest.get("donation_id");
                if (donationId != null) {
                    donorRepository.markDonationUsed(donationId); // flips Available → Used (once)
                }
            }
        }

        return "redirect:/admin/donors";
    }
}
