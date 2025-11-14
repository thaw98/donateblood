package com.grppj.donateblood.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.DonorAppointmentBean;
import com.grppj.donateblood.model.AppointmentStatus;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonorAppointmentRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.repository.UserMessageRepository;

@Controller
@RequestMapping("/admin")
public class AppointmentAdminController {

    @Autowired private DonorAppointmentRepository apptRepo;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BloodTypeRepository bloodTypeRepository;
    @Autowired private UserMessageRepository userMessageRepo;

    /** Allowed roles for appointment: donor(3) and recipient(4). */
    private static final int ROLE_DONOR = 3;
    private static final int ROLE_RECIPIENT = 4;
    
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ---------- helpers ----------
    private Integer resolveHospitalId(HttpSession session) {
        Object v = session == null ? null : session.getAttribute("HOSPITAL_ID");
        if (v instanceof Integer) return (Integer) v;
        if (v != null) {
            try { return Integer.valueOf(String.valueOf(v)); } catch (NumberFormatException ignore) {}
        }
        // fallback: first hospital (keeps UI working if session isn't set)
        return hospitalRepository.findAll().stream().findFirst()
                .map(h -> h.getId())
                .orElse(null);
    }

    private Map<Integer, String> buildBloodTypeMap() {
        Map<Integer, String> m = new LinkedHashMap<>();
        m.put(1, "A+"); m.put(2, "A-"); m.put(3, "B+"); m.put(4, "B-");
        m.put(5, "O+"); m.put(6, "O-"); m.put(7, "AB+"); m.put(8, "AB-");
        return m;
    }

    private void populateAddDonorPage(Model model, HttpSession session, String errorMessage) {
        model.addAttribute("donor", new com.grppj.donateblood.model.UserBean());
        var hospitals = hospitalRepository.findAll();
        model.addAttribute("hospital", hospitals.isEmpty() ? null : hospitals.get(0));
        model.addAttribute("bloodTypes", bloodTypeRepository.findAll());
        model.addAttribute("active", "add-donor");

        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);

        if (errorMessage != null && !errorMessage.isBlank()) {
            model.addAttribute("searchError", errorMessage);
        }
    }

    private boolean isEligibleRole(Integer roleId) {
        return roleId != null && (roleId == ROLE_DONOR || roleId == ROLE_RECIPIENT);
    }

    // ---------- routes ----------

    @GetMapping("/appointments")
    public String oldPathRedirect() {
        return "redirect:/admin/donors/appointments";
    }

    /** List appointments for the logged-in hospital (from session). */
    @GetMapping("/donors/appointments")
    public String list(Model model, HttpSession session) {
        if (session.getAttribute("loginuser") == null) {
            return "redirect:/login";
        }

        apptRepo.expirePendingOlderThanHours(24);

        Integer hospitalId = resolveHospitalId(session);
        model.addAttribute("appointments",
                hospitalId == null ? java.util.List.of() : apptRepo.listForHospital(hospitalId));

        model.addAttribute("active", "appointments");

        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);

        return "admin/admin-appointments";
    }

    /** Mark COMPLETED (only if still PENDING) and create 1 Available unit. */
    @PostMapping("/donors/appointments/{id}/complete")
    public String approve(@PathVariable int id, HttpSession session) {
        int changed = apptRepo.updateStatusIfPending(id, AppointmentStatus.completed);
        if (changed > 0) {
            DonationBean d = new DonationBean();
            d.setBloodUnit(1);
            d.setDonationDate(LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            d.setStatus("Available");
            d.setDonorAppointmentId(id);
            donorRepository.addDonation(d); // updates blood_stock
            
            
         // 2️⃣ New: Send message to donor
            DonorAppointmentBean appt = apptRepo.findMessageById(id);
            if (appt != null) {
                Integer hospitalIdObj = (Integer) session.getAttribute("HOSPITAL_ID");
                if (hospitalIdObj != null) {
                    int donorId = appt.getUserId();
                    int hospitalId = hospitalIdObj;
                    String message = "✅ Your donation appointment has been successfully completed. Thank you for donating blood!";
                    userMessageRepo.sendMessage(hospitalId, donorId, message); 
                } else {
                    System.out.println("⚠️ HOSPITAL_ID not found in session");
                }
            }
        }
        return "redirect:/admin/donors/appointments";
    }

    /** Cancel (only if still PENDING) and notify donor */
    @PostMapping("/donors/appointments/{id}/cancel")
    public String reject(@PathVariable int id, HttpSession session) {
        int changed = apptRepo.updateStatusIfPending(id, AppointmentStatus.cancelled);
        if (changed > 0) {
            // 2️⃣ Send message to donor
            DonorAppointmentBean appt = apptRepo.findMessageById(id);
            if (appt != null) {
                Integer hospitalIdObj = (Integer) session.getAttribute("HOSPITAL_ID");
                if (hospitalIdObj != null) {
                    int donorId = appt.getUserId();
                    int hospitalId = hospitalIdObj;
                    String message = "❌ Your donation appointment has been cancelled. Please contact the hospital for more details.";
                    userMessageRepo.sendMessage(hospitalId, donorId, message); 
                } else {
                    System.out.println("⚠️ HOSPITAL_ID not found in session");
                }
            }
        }
        return "redirect:/admin/donors/appointments";
    }


    // ----- Search existing donor by email from Add Donor page -----
    @PostMapping("/donors/appointments/search")
    public String searchExistingDonorByEmail(@RequestParam("email") String email,
                                             Model model,
                                             HttpSession session) {
        String q = email == null ? "" : email.trim();
        if (q.isEmpty()) {
            populateAddDonorPage(model, session, "Please enter an email.");
            return "admin/add-donor-admin";
        }

        var user = donorRepository.findByEmail(q); // now returns role 3 or 4
        if (user == null || !isEligibleRole(user.getRoleId())) {
            populateAddDonorPage(model, session, "No donor found with that email.");
            return "admin/add-donor-admin";
        }

        return "redirect:/admin/donors/appointments/new/" + user.getId();
    }

    /** Show "new appointment" form for existing donor/recipient using session hospital. */
    @GetMapping("/donors/appointments/new/{userId}")
    public String showNewAppointmentForm(@PathVariable int userId, Model model, HttpSession session) {
        if (session.getAttribute("loginuser") == null) {
            return "redirect:/login";
        }

        var user = donorRepository.getDonorById(userId);
        if (user == null || !isEligibleRole(user.getRoleId())) {
            populateAddDonorPage(model, session, "No donor found with that email.");
            return "admin/add-donor-admin";
        }

        Integer hid = resolveHospitalId(session);

        DonorAppointmentBean appt = new DonorAppointmentBean();
        appt.setUserId(userId);
        appt.setHospitalId(hid);
        appt.setBloodTypeId(user.getBloodTypeId()); // may be null → form will require selection

        model.addAttribute("donor", user);
        model.addAttribute("appt", appt);
        model.addAttribute("bloodTypeMap", buildBloodTypeMap());
        model.addAttribute("active", "appointments");

        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);

        return "admin/appointment-new";
    }

    /**
     * Create a PENDING appointment for the session hospital,
     * enforcing a 4-month (120-day) cooldown since the last COMPLETED donation.
     */
    @PostMapping("/donors/appointments/new")
    public String createAppointment(@ModelAttribute("appt") DonorAppointmentBean appt,
                                    Model model, HttpSession session) {

        // -------- age gate (recipient-specific; remove the role check to apply to donors too) --------
        var user = donorRepository.getDonorById(appt.getUserId()); // has dateOfBirth & roleId
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate dob   = user.getDateOfBirth();
        
        

        if (user.getRoleId() == ROLE_RECIPIENT) { // <- keep if you only want this for recipients
            int years = (dob == null) ? 0 : Period.between(dob, today).getYears();
            if (dob == null || years < 18) {
                model.addAttribute("donor", user);
                model.addAttribute("appt", appt);
                model.addAttribute("bloodTypeMap", buildBloodTypeMap());
                model.addAttribute("active", "appointments");
                model.addAttribute("formError", "Donor must be at least 18 Years old to donate blood.");

                String adminName = (String) session.getAttribute("ADMIN_NAME");
                String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
                model.addAttribute("userName", adminName != null ? adminName : "Admin");
                model.addAttribute("avatarUrl", avatarUrl);
                return "admin/appointment-new";
            }
            
            
        }

        // -------- still require blood type if donor/recipient doesn't have one --------
        if (appt.getBloodTypeId() == null) {
            model.addAttribute("donor", user);
            model.addAttribute("appt", appt);
            model.addAttribute("bloodTypeMap", buildBloodTypeMap());
            model.addAttribute("active", "appointments");
            model.addAttribute("formError", "Blood type is required.");

            String adminName = (String) session.getAttribute("ADMIN_NAME");
            String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
            model.addAttribute("userName", adminName != null ? adminName : "Admin");
            model.addAttribute("avatarUrl", avatarUrl);
            return "admin/appointment-new";
        }

        // -------- 4-month (120 days) cooldown stays enforced --------
        LocalDate lastDonationDate = apptRepo.findLastDonationDateByUserId(appt.getUserId());
        if (lastDonationDate != null) {
            long daysSinceLast = java.time.temporal.ChronoUnit.DAYS.between(lastDonationDate, today);
            if (daysSinceLast < 120) {
                model.addAttribute("donor", user);
                model.addAttribute("appt", appt);
                model.addAttribute("bloodTypeMap", buildBloodTypeMap());
                model.addAttribute("active", "appointments");
                model.addAttribute(
                    "formError",
                    "This donor cannot donate blood yet! Please wait at least 4 months since your last donation (" +
                    lastDonationDate.format(DMY) + ")."
                );

                String adminName = (String) session.getAttribute("ADMIN_NAME");
                String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
                model.addAttribute("userName", adminName != null ? adminName : "Admin");
                model.addAttribute("avatarUrl", avatarUrl);
                return "admin/appointment-new";
            }
        }
        
        

        Integer hospitalId = resolveHospitalId(session);
        int roleId = donorRepository.findUserRoleId(appt.getUserId());

        // 1) Create COMPLETED appointment stamped with current date/time
        int apptId = apptRepo.createCompletedAppointmentNow(
                appt.getUserId(), roleId, hospitalId, appt.getBloodTypeId()
        );

        // 2) Create the donation (1 unit, Available) tied to that appointment → updates blood_stock
        DonationBean d = new DonationBean();
        d.setBloodUnit(1);
        d.setDonationDate(
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        d.setStatus("Available");
        d.setDonorAppointmentId(apptId);
        donorRepository.addDonation(d);

        // 3) Notify donor
        var apptMsg = apptRepo.findMessageById(apptId);
        if (apptMsg != null) {
            Integer hid = (Integer) session.getAttribute("HOSPITAL_ID");
            if (hid != null) {
                String message = "✅ Your donation appointment was created and completed right now. Thank you for donating blood!";
                userMessageRepo.sendMessage(hid, apptMsg.getUserId(), message);
            }
        }

        return "redirect:/admin/donors/appointments";
    }
}
