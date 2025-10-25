// src/main/java/com/grppj/donateblood/controller/AppointmentAdminController.java
package com.grppj.donateblood.controller;

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

@Controller
@RequestMapping("/admin")
public class AppointmentAdminController {

    @Autowired private DonorAppointmentRepository apptRepo;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BloodTypeRepository bloodTypeRepository;

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

    // ---------- routes ----------

    @GetMapping("/appointments")
    public String oldPathRedirect() {
        return "redirect:/admin/donors/appointments";
    }

    /** List appointments for the logged-in hospital (from session). */
    @GetMapping("/donors/appointments")
    public String list(Model model, HttpSession session) {
        // ✅ Check if logged in
        if (session.getAttribute("loginuser") == null) {
            return "redirect:/login";
        }

        apptRepo.expirePendingOlderThanHours(24);

        Integer hospitalId = resolveHospitalId(session);
        model.addAttribute("appointments",
                hospitalId == null ? java.util.List.of() : apptRepo.listForHospital(hospitalId));

        model.addAttribute("active", "appointments");
        
        // ✅ Add admin name and avatar
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);

        return "admin/admin-appointments";
    }

    /** Mark COMPLETED (only if still PENDING) and create 1 Available unit. */
    @PostMapping("/donors/appointments/{id}/complete")
    public String approve(@PathVariable int id) {
        int changed = apptRepo.updateStatusIfPending(id, AppointmentStatus.completed);
        if (changed > 0) {
            DonationBean d = new DonationBean();
            d.setBloodUnit(1);
            d.setDonationDate(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            d.setStatus("Available");
            d.setDonorAppointmentId(id);
            donorRepository.addDonation(d); // this already updates blood_stock in your repo
        }
        return "redirect:/admin/donors/appointments";
    }

    /** Cancel (only if still PENDING). */
    @PostMapping("/donors/appointments/{id}/cancel")
    public String reject(@PathVariable int id) {
        apptRepo.updateStatusIfPending(id, AppointmentStatus.cancelled);
        return "redirect:/admin/donors/appointments";
    }

    // ----- Search existing donor by email from Add Donor page -----
    @PostMapping("/donors/appointments/search")
    public String searchExistingDonorByEmail(@RequestParam("email") String email,
                                             Model model,
                                             HttpSession session) {
        String q = email == null ? "" : email.trim();
        if (q.isEmpty()) {
            model.addAttribute("donor", new com.grppj.donateblood.model.UserBean());
            model.addAttribute("hospital",
                    hospitalRepository.findAll().isEmpty() ? null : hospitalRepository.findAll().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.findAll());
            model.addAttribute("active", "add-donor");
            model.addAttribute("searchError", "Please enter an email.");
            
            // ✅ Add admin name and avatar
            String adminName = (String) session.getAttribute("ADMIN_NAME");
            String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
            model.addAttribute("userName", adminName != null ? adminName : "Admin");
            model.addAttribute("avatarUrl", avatarUrl);
            
            return "admin/add-donor-admin";
        }

        var donor = donorRepository.findByEmail(q);
        if (donor == null) {
            model.addAttribute("donor", new com.grppj.donateblood.model.UserBean());
            model.addAttribute("hospital",
                    hospitalRepository.findAll().isEmpty() ? null : hospitalRepository.findAll().get(0));
            model.addAttribute("bloodTypes", bloodTypeRepository.findAll());
            model.addAttribute("active", "add-donor");
            model.addAttribute("searchError", "No donor found with that email.");
            
            // ✅ Add admin name and avatar
            String adminName = (String) session.getAttribute("ADMIN_NAME");
            String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
            model.addAttribute("userName", adminName != null ? adminName : "Admin");
            model.addAttribute("avatarUrl", avatarUrl);
            
            return "admin/add-donor-admin";
        }

        return "redirect:/admin/donors/appointments/new/" + donor.getId();
    }

    /** Show "new appointment" form for existing donor using session hospital. */
    @GetMapping("/donors/appointments/new/{userId}")
    public String showNewAppointmentForm(@PathVariable int userId, Model model, HttpSession session) {
        // ✅ Check if logged in
        if (session.getAttribute("loginuser") == null) {
            return "redirect:/login";
        }

        var donor = donorRepository.getDonorById(userId);
        if (donor == null) return "redirect:/admin/donors";

        Integer hid = resolveHospitalId(session);

        DonorAppointmentBean appt = new DonorAppointmentBean();
        appt.setUserId(userId);
        appt.setHospitalId(hid);                 // ← use session hospital
        appt.setBloodTypeId(donor.getBloodTypeId());

        model.addAttribute("donor", donor);
        model.addAttribute("appt", appt);
        model.addAttribute("bloodTypeMap", buildBloodTypeMap());
        model.addAttribute("active", "appointments");
        
        // ✅ ADD: Admin name and avatar for topnav
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);
        
        return "admin/appointment-new";
    }

    /** Create a PENDING appointment for the session hospital. */
    @PostMapping("/donors/appointments/new")
    public String createAppointment(@ModelAttribute("appt") DonorAppointmentBean appt,
                                    Model model, HttpSession session) {

        boolean invalid = (appt.getDate() == null || appt.getDate().isBlank()
                        || appt.getTime() == null || appt.getTime().isBlank());

        if (invalid) {
            var donor = donorRepository.getDonorById(appt.getUserId());
            model.addAttribute("donor", donor);
            model.addAttribute("appt", appt);
            model.addAttribute("bloodTypeMap", buildBloodTypeMap());
            model.addAttribute("active", "appointments");
            model.addAttribute("formError", "Date and time are required.");
            
            // ✅ ADD: Admin name and avatar for error page
            String adminName = (String) session.getAttribute("ADMIN_NAME");
            String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
            model.addAttribute("userName", adminName != null ? adminName : "Admin");
            model.addAttribute("avatarUrl", avatarUrl);
            
            return "admin/appointment-new";
        }

        // Ensure hospital comes from session
        Integer hid = resolveHospitalId(session);
        appt.setHospitalId(hid);

        apptRepo.createAppointment(appt); // creates PENDING
        return "redirect:/admin/donors/appointments";
    }
}
