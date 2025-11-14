package com.grppj.donateblood.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.grppj.donateblood.model.User;
import com.grppj.donateblood.repository.LoginRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.model.Hospital;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private LoginRepository loginRepo;

    @Autowired
    private HospitalRepository hospitalRepo;

    @GetMapping("/login")
    public ModelAndView showLoginForm(HttpSession session, Model model) {
        // Check if there is a password set success message
        String successMsg = (String) session.getAttribute("passwordSetSuccess");
        if (successMsg != null) {
            model.addAttribute("success", successMsg); // pass to view
            session.removeAttribute("passwordSetSuccess"); // clear after reading
        }

        return new ModelAndView("login", "user", new User());
    }


    @PostMapping("/login")
    public ModelAndView doLogin(@ModelAttribute("user") User userInput,
                                HttpSession session,
                                RedirectAttributes ra) {
        List<User> users = loginRepo.loginUser(userInput.getEmail(), userInput.getPassword());
        if (users.isEmpty()) {
            ra.addFlashAttribute("msg", "Incorrect email, or password");
            return new ModelAndView("redirect:/login");
        }

        User user = users.get(0);
        session.setAttribute("loginuser", user);
        session.setAttribute("loggedUser", user);

        String role = (user.getRole() != null) ? user.getRole().getRole().toLowerCase() : "";
        session.setAttribute("activeRole", role);
        
        if ("admin".equals(role) && !user.isVerified()) {
            session.setAttribute("pendingAdminForPassword", user);
            ra.addFlashAttribute("msg", "Please log in to your Gmail account to verify your admin email.");

            String email = user.getEmail();
            String gmailLoginUrl = "https://accounts.google.com/ServiceLogin?Email=" + email;

            return new ModelAndView("redirect:" + gmailLoginUrl);
        }

        session.setAttribute("loginuser", user);
        
        // ✅ NEW: store admin identifiers for later FK insert
        session.setAttribute("ADMIN_ID", user.getId());
        // helpful but optional because controller re-derives it; still set if available
        if (user.getRole() != null) {
            session.setAttribute("ADMIN_ROLE_ID", user.getRole().getId());
        }


        // ✅ ADD: Store admin name and avatar in session
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            session.setAttribute("ADMIN_NAME", user.getUsername());
        } else if (user.getEmail() != null) {
            // Fallback to email if username is null
            session.setAttribute("ADMIN_NAME", user.getEmail().split("@")[0]);
        }
        
        // Set hospital context for admin/superadmin
        if ("admin".equals(role) || "superadmin".equals(role)) {
            Integer hospitalId = loginRepo.findHospitalIdForAdmin(user.getId());

            // Fallback: first hospital in DB
            if (hospitalId == null) {
                var hospitals = hospitalRepo.findAll();
                if (!hospitals.isEmpty()) {
                    hospitalId = hospitals.get(0).getId();
                }
            }

            if (hospitalId != null) {
                session.setAttribute("HOSPITAL_ID", hospitalId);
                Hospital hospital = hospitalRepo.findById(hospitalId);
                if (hospital != null) {
                    session.setAttribute("HOSPITAL_NAME", hospital.getHospitalName());
                }
            }
            
        
        }

        switch (role) {
            case "donor":      return new ModelAndView("redirect:/index");
            case "receiver":   return new ModelAndView("redirect:/indexR");
            case "admin":      return new ModelAndView("redirect:/admin");
            case "superadmin":
                session.setAttribute("loginuser", user);
                session.setAttribute("superadmin", user);
                return new ModelAndView("redirect:/superadmin");

            default:
                ra.addFlashAttribute("msg", "Unknown role");
                return new ModelAndView("redirect:/login");
        }
    }
}
