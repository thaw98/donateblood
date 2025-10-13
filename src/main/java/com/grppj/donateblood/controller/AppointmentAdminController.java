package com.grppj.donateblood.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.repository.DonorAppointmentRepository;
import com.grppj.donateblood.repository.DonorRepository;

@Controller
@RequestMapping("/admin")
public class AppointmentAdminController {

    @Autowired private DonorAppointmentRepository apptRepo;
    @Autowired private DonorRepository donorRepository;

    // keep old URL working
    @GetMapping("/appointments")
    public String oldPathRedirect() {
        return "redirect:/admin/donors/appointments";
    }

    /** Show ALL appointments for the hospital. */
    @GetMapping("/donors/appointments")
    public String list(Model model) {
        model.addAttribute("appointments", apptRepo.listForHospital(1)); // hospital_id = 1
        model.addAttribute("active", "appointments");
        return "admin/admin-appointments";
    }

    /** Approve only if still PENDING; then create exactly one Available donation unit. */
    @PostMapping("/donors/appointments/{id}/approve")
    public String approve(@PathVariable int id) {
        int changed = apptRepo.updateStatusIfPending(id, "approved");
        if (changed > 0) {
            DonationBean d = new DonationBean();
            d.setBloodUnit(1);
            d.setDonationDate(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            d.setStatus("Available");
            d.setDonorAppointmentId(id);
            donorRepository.addDonation(d);
        }
        return "redirect:/admin/donors/appointments";
    }

    /** Reject only if still PENDING. */
    @PostMapping("/donors/appointments/{id}/reject")
    public String reject(@PathVariable int id) {
        apptRepo.updateStatusIfPending(id, "rejected");
        return "redirect:/admin/donors/appointments";
    }

    /** Optional alias – treat cancel as reject of a pending request. */
    @PostMapping("/donors/appointments/{id}/cancel")
    public String cancel(@PathVariable int id) {
        apptRepo.updateStatusIfPending(id, "rejected");
        return "redirect:/admin/donors/appointments";
    }
}
