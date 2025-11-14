package com.grppj.donateblood.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import com.grppj.donateblood.model.RoleBean;
import com.grppj.donateblood.model.User;
import com.grppj.donateblood.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonationRepository;
import com.grppj.donateblood.repository.FulfillmentRepository;
import com.grppj.donateblood.repository.HospitalRepository;




@Controller
public class IndexController {

    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BloodTypeRepository bloodTypeRepo;
    @Autowired private FulfillmentRepository fulfillmentRepository;
    @Autowired private DonationRepository donationRepository;    

    private void addStats(Model model) {
        // Lives Saved ✅
        Integer livesSaved = fulfillmentRepository.getTotalUnitsUsed();

        // Regular Donors ✅
        Integer regularDonors = donationRepository.countRegularDonors();

        // Donation Centers ✅
        Integer donationCenters = hospitalRepository.countHospitals(); // we'll add this method

        model.addAttribute("livesSaved", livesSaved);
        model.addAttribute("regularDonors", regularDonors);
        model.addAttribute("donationCenters", donationCenters);
    }


    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("hospitalList", hospitalRepository.findAll());
        addStats(model);
        return "home";
    }

    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("hospitalList", hospitalRepository.findAll());
        addStats(model);
        return "index";
    }

    @GetMapping("/indexR")
    public String indexR(Model model) {
        model.addAttribute("hospitalList", hospitalRepository.findAll());
        addStats(model);
        return "indexR";
    }
    
    @GetMapping("/about")
    public String about(HttpSession session, Model model) {
        // Add stats if needed
        addStats(model);

        // Determine header type
        User user = (User) session.getAttribute("loginuser");
        if (user == null) {
            model.addAttribute("headerType", "public");  // Not logged in
        } else {
            String role = (user.getRole() != null) ? user.getRole().getRole().toLowerCase() : "";
            if ("donor".equals(role)) model.addAttribute("headerType", "donor"); // headerL
            else if ("receiver".equals(role)) model.addAttribute("headerType", "receiver"); // headerR
            else model.addAttribute("headerType", "public");
        }

        return "about";
    }
    // ✅ Registration form — show only Donor and Receiver
    @GetMapping("/register")
    public String register(Model model) {
        User user = new User();
        user.setRole(new RoleBean());
        model.addAttribute("userObj", user);
        

        // Filter roles by role name
        List<RoleBean> roleList = userRepository.getAllRoles()
                .stream()
                .filter(role -> role.getRole().equalsIgnoreCase("Donor")
                             || role.getRole().equalsIgnoreCase("Receiver"))
                .collect(Collectors.toList());

        model.addAttribute("bloodTypes", bloodTypeRepo.findAll());
        model.addAttribute("roleList", roleList);
        
        return "register";
    }

    // ✅ Registration submit
    @PostMapping("/register")
    public String doRegister(@ModelAttribute("userObj") User user, Model model) {
        try {
            if (userRepository.emailExists(user.getEmail())) {
                model.addAttribute("errorMsg", "Email already registered!");

                List<RoleBean> roleList = userRepository.getAllRoles()
                        .stream()
                        .filter(role -> role.getRole().equalsIgnoreCase("Donor")
                                     || role.getRole().equalsIgnoreCase("Receiver"))
                        .collect(Collectors.toList());
                model.addAttribute("roleList", roleList);
                return "register";
            }

            
         // ✅ Convert uploaded file to byte array before saving
            MultipartFile file = user.getFilePart();
            if (file != null && !file.isEmpty()) {
                user.setImageBytes(file.getBytes());
            }
            
            // ✅ Default to Donor (id = 3)
            if (user.getRole() == null || user.getRole().getId() == 0) {
                user.setRole(userRepository.getRoleById(3));
            }

            int result = userRepository.doRegister(user);

            if (result > 0) {
                return "redirect:/login?success";
            } else {
                model.addAttribute("errorMsg", "Registration failed. Please try again.");

                List<RoleBean> roleList = userRepository.getAllRoles()
                        .stream()
                        .filter(role -> role.getRole().equalsIgnoreCase("Donor")
                                     || role.getRole().equalsIgnoreCase("Receiver"))
                        .collect(Collectors.toList());
                model.addAttribute("roleList", roleList);
                return "register";
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMsg", "Registration error: " + e.getMessage());

            List<RoleBean> roleList = userRepository.getAllRoles()
                    .stream()
                    .filter(role -> role.getRole().equalsIgnoreCase("Donor")
                                 || role.getRole().equalsIgnoreCase("Receiver"))
                    .collect(Collectors.toList());
            model.addAttribute("roleList", roleList);
            return "register";
        }
    }
    
}
