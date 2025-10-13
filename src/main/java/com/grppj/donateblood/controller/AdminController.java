// src/main/java/com/grppj/donateblood/controller/AdminController.java
package com.grppj.donateblood.controller;

import com.grppj.donateblood.repository.DashboardRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DashboardRepository dashboardRepo;

    public AdminController(DashboardRepository dashboardRepo) {
        this.dashboardRepo = dashboardRepo;
    }

    // serve /admin, /admin/ and /admin/dashboard
    @GetMapping({ "", "/", "/dashboard" })
    public String dashboard(Model model) {
        // sidebar + page chrome
        model.addAttribute("active", "admin");
        model.addAttribute("userName", "Admin");
        model.addAttribute("avatarUrl", null); // or a URL

        // counters (JdbcTemplate via DashboardRepository)
        model.addAttribute("totalBloodDonated", dashboardRepo.totalBloodDonatedUnits());
        model.addAttribute("completedDonors",   dashboardRepo.completedDonors());
        model.addAttribute("pendingDonors",     dashboardRepo.pendingDonors());
        model.addAttribute("totalAppointments", dashboardRepo.totalAppointments());

        // resolves to templates/admin/dashboard.html
        return "admin/dashboard";
    }
}
