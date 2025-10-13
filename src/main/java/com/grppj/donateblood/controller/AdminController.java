package com.grppj.donateblood.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // /admin  or /admin/
    @GetMapping({ "", "/" })
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard");
        model.addAttribute("active", "dashboard");

        // demo data for the top bar
        model.addAttribute("userName", "Admin");
        model.addAttribute("notifications", java.util.List.of());
        model.addAttribute("avatarUrl", null); // or a real URL

        // Thymeleaf will resolve to src/main/resources/templates/admin/admin-dashboard.html
        return "admin/dashboard";
    }
}
