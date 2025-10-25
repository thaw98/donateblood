package com.grppj.donateblood.controller;

import com.grppj.donateblood.repository.DashboardRepository;
import jakarta.servlet.http.HttpSession;
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

    @GetMapping({ "", "/", "/dashboard" })
    public String dashboard(Model model, HttpSession session) {
        
        // ✅ Check if user is logged in
        Object loginUser = session.getAttribute("loginuser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // ✅ Get stored session attributes
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        String avatarUrl = (String) session.getAttribute("ADMIN_AVATAR_URL");
        String hospitalName = (String) session.getAttribute("HOSPITAL_NAME");

        // ✅ Pass to topnav fragment
        model.addAttribute("active", "admin");
        model.addAttribute("userName", adminName != null ? adminName : "Admin");
        model.addAttribute("avatarUrl", avatarUrl);

        // Dashboard counters
        model.addAttribute("totalBloodDonated", dashboardRepo.totalBloodDonatedUnits());
        model.addAttribute("completedDonors", dashboardRepo.completedDonors());
        model.addAttribute("pendingDonors", dashboardRepo.pendingDonors());
        model.addAttribute("totalAppointments", dashboardRepo.totalAppointments());

        return "admin/dashboard";
    }
}
