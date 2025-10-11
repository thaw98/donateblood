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
        return "admin-appointments";
    }

    // ✅ POST /admin/donors/appointments/{id}/approve
    @PostMapping("/donors/appointments/{id}/approve")
    public String approve(@PathVariable int id) {
        Integer adminId = 1; // TODO: replace with logged-in admin

        // get the appointment
        var appt = apptRepo.findById(id);
        int userId      = ((Number) appt.get("user_id")).intValue();
        int userRoleId  = ((Number) appt.get("user_role_id")).intValue();
        int hospitalId  = ((Number) appt.get("hospital_id")).intValue();
        int bloodTypeId = ((Number) appt.get("blood_type_id")).intValue();

        // 1) mark appointment approved
        apptRepo.updateStatus(id, "approved", adminId);

        // 2) create the donation row (status Available)
        DonationBean d = new com.grppj.donateblood.model.DonationBean();
        d.setBloodUnit(1);
        d.setDonationDate(java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        d.setStatus("Available");
        d.setUserId(userId);
        d.setUserRoleId(userRoleId);
        d.setHospitalId(hospitalId);
        donorRepository.addDonation(d);

        // 3) increase stock
        bloodStockRepository.increaseStock(hospitalId, bloodTypeId, 1, adminId, /*admin role*/1);

        return "redirect:/admin/donors/appointments";
    }


    // ✅ POST /admin/donors/appointments/{id}/reject
    @PostMapping("/donors/appointments/{id}/reject")
    public String reject(@PathVariable int id) {
        apptRepo.updateStatus(id, "rejected");
        // or apptRepo.updateStatusIfHospital(id, "rejected", adminId, 1);
        return "redirect:/admin/donors/appointments";
    }
}