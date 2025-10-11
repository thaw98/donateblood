package com.grppj.donateblood.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.repository.BloodStockRepository;
import com.grppj.donateblood.repository.DonorAppointmentRepository;
import com.grppj.donateblood.repository.DonorRepository;

@Controller
@RequestMapping("/admin") // final path will be /admin/...
public class AppointmentAdminController {

    @Autowired 
    private DonorAppointmentRepository apptRepo;
    
    @Autowired 
    private BloodStockRepository bloodStockRepository;
    
    @Autowired 
    private DonorRepository donorRepository;


    // optional: keep old URL working
    @GetMapping("/appointments")
    public String oldPathRedirect() {
        return "redirect:/admin/donors/appointments";
    }

    // ✅ TARGET URL: /admin/donors/appointments
    @GetMapping("/donors/appointments")
    public String list(Model model) {
        model.addAttribute("appointments", apptRepo.listForHospital(1)); // only hospital_id = 1
        return "admin/admin-appointments";
    }

    // ✅ POST /admin/donors/appointments/{id}/approve
    @PostMapping("/donors/appointments/{id}/approve")
    public String approve(@PathVariable int id) {

        // Get appointment (you already have apptRepo)
        java.util.Map<String,Object> appt = apptRepo.findById(id);
        int userId     = ((Number) appt.get("user_id")).intValue();
        int userRoleId = ((Number) appt.get("user_role_id")).intValue(); // donor role == 2
        int hospitalId = ((Number) appt.get("hospital_id")).intValue();

        // 1) mark appointment approved
        apptRepo.updateStatus(id, "approved");

        // 2) create one Available donation
        com.grppj.donateblood.model.DonationBean d = new com.grppj.donateblood.model.DonationBean();
        d.setBloodUnit(1);
        d.setDonationDate(java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        d.setStatus("Available");
        d.setUserId(userId);
        d.setUserRoleId(userRoleId);
        d.setHospitalId(hospitalId);
        donorRepository.addDonation(d);

        // Back to appointments (or redirect to stock for this hospital)
        return "redirect:/admin/donors/appointments";
        // return "redirect:/admin/bloodstock?hid=" + hospitalId;
    }

    // ✅ POST /admin/donors/appointments/{id}/reject
    @PostMapping("/donors/appointments/{id}/reject")
    public String reject(@PathVariable int id) {
        apptRepo.updateStatus(id, "rejected");
        // or apptRepo.updateStatusIfHospital(id, "rejected", adminId, 1);
        return "redirect:/admin/donors/appointments";
    }
}