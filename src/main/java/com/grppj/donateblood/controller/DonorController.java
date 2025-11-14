package com.grppj.donateblood.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.grppj.donateblood.model.AppointmentStatus;
import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.UserBean;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonorAppointmentRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.repository.UserMessageRepository;
import com.grppj.donateblood.service.EmailService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class DonorController {
	
	@Autowired EmailService emailService;

    /** donors have role_id = 3 */
    private static final int DONOR_ROLE = 3;

    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BloodTypeRepository bloodTypeRepository;
    @Autowired private DonorAppointmentRepository apptRepo;
    @Autowired private UserMessageRepository userMessageRepository;

    /** Use direct JDBC here to update phone only (avoid mass donation status update). */
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final Map<Integer, String> bloodTypeMap = new HashMap<>();
    static {
        bloodTypeMap.put(1, "A+");
        bloodTypeMap.put(2, "A-");
        bloodTypeMap.put(3, "B+");
        bloodTypeMap.put(4, "B-");
        bloodTypeMap.put(5, "O+");
        bloodTypeMap.put(6, "O-");
        bloodTypeMap.put(7, "AB+");
        bloodTypeMap.put(8, "AB-");
    }

    private static final Pattern EMAIL_RE =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // ---- helper: resolve current hospital id from session, else fallback ----
    private int resolveHospitalId(HttpSession session) {
        Object v = session == null ? null : session.getAttribute("HOSPITAL_ID");
        if (v instanceof Integer) return (Integer) v;
        if (v != null) {
            try { return Integer.parseInt(String.valueOf(v)); } catch (NumberFormatException ignore) {}
        }
        // Fallback: first hospital (keeps page working if session not set yet)
        return hospitalRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hospitals configured"))
                .getId();
    }

    // ===== Add Donor (GET) =====
    @GetMapping("/donors/add")
    public String showAddDonorForm(Model model, HttpSession session) {
        // ✅ Check if logged in
        if (session.getAttribute("loginuser") == null) {
            return "redirect:/login";
        }

        int hospitalId = resolveHospitalId(session);
        var hospital = hospitalRepository.findById(hospitalId);

        UserBean donor = new UserBean();
        model.addAttribute("hospital", hospital);
        model.addAttribute("hospitalId", hospitalId);
        model.addAttribute("donor", donor);
        model.addAttribute("active", "add-donor");
        model.addAttribute("bloodTypes", bloodTypeRepository.findAll());
        model.addAttribute("maxDob",
            LocalDate.now().minusYears(18).format(DateTimeFormatter.ISO_DATE));
        
        // ✅ Add admin name and avatar for topnav
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);
        
        return "admin/add-donor-admin";
    }

    // ===== Add Donor (POST) =====
    @PostMapping("/donors/add")
    public String addDonor(@Valid @ModelAttribute("donor") UserBean donor,
                           BindingResult bindingResult,
                           Model model,
                           HttpSession session) {

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
        } else {
            String p = phone.trim();
            if (!p.matches("^09\\d{7,11}$")) {
                bindingResult.addError(new FieldError("donor", "phone",
                        "Phone must start with 09 and be 9–13 digits."));
            }
        }
        
        // ---- gender (required)
        if (donor.getGender() == null || donor.getGender().trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "gender", "Gender is required."));
        }

        // ---- blood type
        if (donor.getBloodTypeId() == null) {
            bindingResult.addError(new FieldError("donor", "bloodTypeId", "Blood type is required."));
        }

        // ---- address
        if (donor.getAddress() == null || donor.getAddress().trim().isEmpty()) {
            bindingResult.addError(new FieldError("donor", "address", "Address is required."));
        }
        
     // ---- date of birth (required, must be >= 18 years old)
        if (donor.getDateOfBirth() == null) {
            bindingResult.addError(new FieldError("donor", "dateOfBirth",
                    "Date of birth is required."));
        } else {
            LocalDate dob = donor.getDateOfBirth();
            LocalDate mustBeBeforeOrOn = LocalDate.now().minusYears(18);
            if (dob.isAfter(mustBeBeforeOrOn)) {
                bindingResult.addError(new FieldError("donor", "dateOfBirth",
                        "Donor must be at least 18 years old."));
            }
        }

        if (bindingResult.hasErrors()) {
            int hospitalId = resolveHospitalId(session);
            model.addAttribute("hospital", hospitalRepository.findById(hospitalId));
            model.addAttribute("hospitalId", hospitalId);
            model.addAttribute("bloodTypes", bloodTypeRepository.findAll());
            
            // ✅ Add admin name and avatar for topnav (error page)
            String adminName = (String) session.getAttribute("ADMIN_NAME");
            String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
            model.addAttribute("userName", adminName != null ? adminName : "Admin");
            model.addAttribute("avatarUrl", avatarUrl);
            
            return "admin/add-donor-admin";
        }

        String otpPassword = com.grppj.donateblood.util.OTPGenerator.generateOTP(6);  // 6-digit OTP
        donor.setPassword(otpPassword);
        
        // 1) Create donor user
        donor.setRoleId(DONOR_ROLE);        
        int userId = donorRepository.addDonor(donor);

        emailService.sendPassword(donor.getEmail(), otpPassword);
        
        // 2) Resolve role/hospital dynamically (NOT static)
        int actualRoleId = donorRepository.findUserRoleId(userId);
        int hospitalId   = resolveHospitalId(session);

        // 3) Create an APPROVED/COMPLETED appointment now (hospital-scoped)
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

    // ===== List donors =====
    @GetMapping("/donors")
    public String showDonorList(Model model, HttpSession session) {
        if (session.getAttribute("loginuser") == null) {
            return "redirect:/login";
        }

        int hospitalId = resolveHospitalId(session);

        List<UserBean> donorList = donorRepository.getDonorsForHospital(DONOR_ROLE, hospitalId);

        Map<Integer, Integer> totalDonations = new HashMap<>();
        for (UserBean d : donorList) {
            int count = donorRepository.countDonations(d.getId());
            totalDonations.put(d.getId(), count);
        }

        // NEW: last donation dates (formatted dd-MM-yyyy)
        Map<Integer, String> lastDonationDates = new HashMap<>();
        var fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        for (UserBean d : donorList) {
            var last = donorRepository.getLastDonationDate(d.getId(), DONOR_ROLE, hospitalId);
            if (last != null) lastDonationDates.put(d.getId(), last.toLocalDate().format(fmt));
        }

        // Sort donors by total donations as you already do
        donorList.sort((a, b) -> Integer.compare(
            totalDonations.getOrDefault(b.getId(), 0),
            totalDonations.getOrDefault(a.getId(), 0)
        ));

        model.addAttribute("donorList", donorList);
        model.addAttribute("totalDonations", totalDonations);

        // NEW: expose to the template
        model.addAttribute("lastDonationDates", lastDonationDates);

        model.addAttribute("active", "donors");
        model.addAttribute("bloodTypeMap", bloodTypeMap);

        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);

        return "admin/donor-list";
    }

    // ===== Edit donor (GET) =====
    @GetMapping("/donors/edit/{id}")
    public String showEditDonorForm(@PathVariable("id") int id, Model model, HttpSession session) {
        // ✅ Check if logged in
        if (session.getAttribute("loginuser") == null) {
            return "redirect:/login";
        }

        UserBean donor = donorRepository.getDonorById(id);
        model.addAttribute("donor", donor);
        model.addAttribute("bloodTypeMap", bloodTypeMap);
        model.addAttribute("bloodTypes", bloodTypeRepository.findAll());
        model.addAttribute("hospitals", hospitalRepository.findAll());
        
        // ✅ Add admin name and avatar for topnav
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);
        
        return "admin/edit-donor-admin";
    }

    // ===== Edit donor (POST) =====
    @PostMapping("/donors/update")
    public String updateDonor(@ModelAttribute("donor") UserBean donor) {
        jdbcTemplate.update("UPDATE `user` SET phone = ? WHERE id = ?", donor.getPhone(), donor.getId());

        if ("Used".equalsIgnoreCase(String.valueOf(donor.getStatus()))) {
            var latest = donorRepository.findLatestDonation(donor.getId(), DONOR_ROLE);
            if (latest != null) {
                Integer donationId = (Integer) latest.get("donation_id");
                if (donationId != null) {
                    donorRepository.markDonationUsed(donationId);
                }
            }
        }
        return "redirect:/admin/donors";
    }

    // ===== Donation History (GET) =====
    @GetMapping("/donors/history/{id}")
    public String showDonationHistory(@PathVariable("id") int id, Model model, HttpSession session) {
        // ✅ Check if logged in
        if (session.getAttribute("loginuser") == null) {
            return "redirect:/login";
        }

        UserBean donor = donorRepository.getDonorById(id);
        List<DonorRepository.DonationHistoryRow> history =
                donorRepository.getDonationHistory(id, DONOR_ROLE);

        model.addAttribute("donor", donor);
        model.addAttribute("history", history);
        model.addAttribute("bloodTypeMap", bloodTypeMap);
        model.addAttribute("active", "donors");
        
        // ✅ Add admin name and avatar for topnav
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);
        
        return "admin/donor-history";
    }
    @PostMapping("/donors/notify")
    public String notifyDonor(
            @RequestParam("userId") int userId,
            @RequestParam(value = "bloodTypeId", required = false) Integer bloodTypeId,
            @RequestParam("apptDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate apptDate,
            @RequestParam("apptTime") String apptTime,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        // DO NOT create any appointment here.
        int hospitalId = resolveHospitalId(session);

        // Send in-app message with suggested date/time (message-only)
        String msg = "Blood is urgently needed. Could you please come and donate on "
                   + apptDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                   + " at " + apptTime + "?";
        userMessageRepository.sendMessageWithAppt(hospitalId, userId, msg, apptDate, apptTime);

        // Flash success in admin dashboard
        String donorName = donorRepository.getDonorById(userId).getUsername();
        redirectAttrs.addFlashAttribute("success",
                "Successfully notified " + donorName + ".");

        return "redirect:/admin/donors";
    }
}
