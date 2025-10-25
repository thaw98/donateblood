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
import com.grppj.donateblood.repository.HospitalRepository;




@Controller
public class IndexController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("hospitalList", hospitalRepository.findAll());
        return "home";
    }

    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("hospitalList", hospitalRepository.findAll());
        return "index";
    }

    @GetMapping("/indexR")
    public String indexR(Model model) {
        model.addAttribute("hospitalList", hospitalRepository.findAll());
        return "indexR";
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
