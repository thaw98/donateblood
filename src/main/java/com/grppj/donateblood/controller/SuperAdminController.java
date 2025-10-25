package com.grppj.donateblood.controller;

import com.grppj.donateblood.model.User;
import com.grppj.donateblood.model.Hospital;
import com.grppj.donateblood.repository.UserRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Random;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private EmailService emailService;

    private final String UPLOAD_DIR = "uploads/hospitals/";

    @GetMapping("")
    public String superadminRoot(HttpSession session) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        return "redirect:/superadmin/dashboard";
    }

    private void ensureUploadDirectoryExists() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        User superadmin = (User) session.getAttribute("superadmin");
        List<User> admins = userRepository.findAllAdmins();
        List<Hospital> hospitals = hospitalRepository.findAllHospitals();

        model.addAttribute("admins", admins);
        model.addAttribute("hospitals", hospitals);
        model.addAttribute("superadmin", superadmin);

        return "superadmin/dashboard";
    }

    // ================= CREATE ADMIN WITH OTP =================
    @GetMapping("/create-admin")
    public String createAdminForm(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        List<Hospital> hospitals = hospitalRepository.findAllHospitals();
        model.addAttribute("hospitals", hospitals);
        return "superadmin/create-admin";
    }

    @PostMapping("/create-admin")
    public String createAdmin(@RequestParam String email,
                              @RequestParam(required = false) Integer hospitalId,
                              HttpSession session,
                              Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        try {
            // Auto-generate username from email
            String username = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "_");

            // Check if email already exists
            if (userRepository.emailExists(email)) {
                model.addAttribute("error", "Email already exists: " + email);
                return reloadCreateAdminData(model);
            }

            // Generate OTP
            String verificationCode = generateVerificationCode();
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);

            // Save admin with OTP
            boolean created = userRepository.createAdmin(username, email, hospitalId, verificationCode);

            if (!created) {
                model.addAttribute("error", "Failed to create admin account!");
                return reloadCreateAdminData(model);
            }

            // Assign hospital if provided
            if (hospitalId != null && hospitalId > 0) {
                User newAdmin = userRepository.findAdminByEmail(email);
                if (newAdmin != null) {
                    userRepository.assignAdminToHospital(newAdmin.getId(), hospitalId);
                }
            }

            // Send welcome email with OTP
            emailService.sendAdminWelcomeEmail(email, verificationCode);

            model.addAttribute("success",
                    "✅ Admin created successfully!<br>Username: " + username +
                    "<br>Email: " + email +
                    "<br>OTP: " + verificationCode +
                    (hospitalId != null ? "<br>Assigned to hospital ID: " + hospitalId : "")
            );

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error: " + e.getMessage());
        }

        return reloadCreateAdminData(model);
    }

    private String reloadCreateAdminData(Model model) {
        List<Hospital> hospitals = hospitalRepository.findAllHospitals();
        model.addAttribute("hospitals", hospitals);
        return "superadmin/create-admin";
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    
    @GetMapping("/assign-admin")
    public String showAssignAdminForm(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        List<User> admins = userRepository.findAllAdmins();
        List<Hospital> hospitals = hospitalRepository.findAllHospitals();
        
        model.addAttribute("admins", admins);
        model.addAttribute("hospitals", hospitals);
        return "superadmin/assign-admin";
    }
    
    @PostMapping("/assign-admin")
    public String assignAdminToHospital(@RequestParam Integer adminId, 
                                       @RequestParam Integer hospitalId,
                                       HttpSession session, 
                                       Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        boolean success = userRepository.assignAdminToHospital(adminId, hospitalId);
        if (success) {
            User admin = userRepository.getUserById(adminId);
            Hospital hospital = hospitalRepository.findById(hospitalId);
            
            if (admin != null && hospital != null) {
                model.addAttribute("success", 
                    "Admin '" + admin.getUsername() + "' successfully assigned to '" + 
                    hospital.getHospitalName() + "'");
                
                emailService.sendAssignmentNotification(admin.getEmail(), hospital.getHospitalName());
            } else {
                model.addAttribute("success", "Admin assigned to hospital successfully!");
            }
        } else {
            model.addAttribute("error", "Failed to assign admin to hospital!");
        }
        
        List<User> admins = userRepository.findAllAdmins();
        List<Hospital> hospitals = hospitalRepository.findAllHospitals();
        model.addAttribute("admins", admins);
        model.addAttribute("hospitals", hospitals);
        
        return "superadmin/assign-admin";
    }
    

    
    @GetMapping("/add-hospital")
    public String addHospitalForm(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        model.addAttribute("hospital", new Hospital());
        return "superadmin/add-hospital";
    }
    
    @PostMapping("/add-hospital")
    public String addHospital(@RequestParam String hospitalName,
                            @RequestParam String address,
                            @RequestParam String phoneNo,
                            @RequestParam String gmail,
                            @RequestParam("profilePicturesFiles") MultipartFile[] profilePicturesFiles,
                            HttpSession session, 
                            Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        try {
            Hospital hospital = new Hospital();
            hospital.setHospitalName(hospitalName);
            hospital.setAddress(address);
            hospital.setPhoneNo(phoneNo);
            hospital.setGmail(gmail);
            
            List<String> uploadedFileNames = new ArrayList<>();
            
            if (profilePicturesFiles != null && profilePicturesFiles.length > 0) {
                ensureUploadDirectoryExists();
                
                for (MultipartFile file : profilePicturesFiles) {
                    if (!file.isEmpty()) {
                        String originalFileName = file.getOriginalFilename();
                        String fileExtension = "";
                        if (originalFileName != null && originalFileName.contains(".")) {
                            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                        }
                        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                        
                        Path filePath = Paths.get(UPLOAD_DIR, uniqueFileName);
                        Files.copy(file.getInputStream(), filePath);
                        
                        uploadedFileNames.add(uniqueFileName);
                    }
                }
            }
            
            hospital.setProfilePictures(uploadedFileNames);
            
            boolean success = hospitalRepository.addHospital(hospital);
            if (success) {
                model.addAttribute("success", "Hospital added successfully with " + uploadedFileNames.size() + " profile pictures!");
            } else {
                model.addAttribute("error", "Failed to add hospital!");
            }
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        
        model.addAttribute("hospital", new Hospital());
        return "superadmin/add-hospital";
    }
    
    
    @GetMapping("/hospital-profile/{id}")
    public String viewHospitalProfile(@PathVariable Integer id, HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        Hospital hospital = hospitalRepository.findById(id);
        List<User> hospitalAdmins = userRepository.findAdminsByHospital(id);
        
        model.addAttribute("hospital", hospital);
        model.addAttribute("admins", hospitalAdmins);
        return "superadmin/hospital-profile";
    }


    // =================== REMAINING CONTROLLER LOGIC (unchanged) ===================

    @GetMapping("/admin-list")
    public String adminList(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        model.addAttribute("admins", userRepository.findAllAdmins());
        model.addAttribute("hospitals", hospitalRepository.findAllHospitals());
        return "superadmin/admin-list";
    }

    @GetMapping("/hospital-list")
    public String hospitalList(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        model.addAttribute("hospitals", hospitalRepository.findAllHospitals());
        return "superadmin/hospital-list";
    }

    @GetMapping("/delete-admin/{id}")
    public String deleteAdmin(@PathVariable Integer id, HttpSession session) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        userRepository.deleteAdmin(id);
        return "redirect:/superadmin/admin-list";
    }

    @GetMapping("/delete-hospital/{id}")
    public String deleteHospital(@PathVariable Integer id, HttpSession session) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        Hospital hospital = hospitalRepository.findById(id);
        if (hospital != null && hospital.getProfilePictures() != null) {
            for (String fileName : hospital.getProfilePictures()) {
                try {
                    Files.deleteIfExists(Paths.get(UPLOAD_DIR, fileName));
                } catch (IOException e) { e.printStackTrace(); }
            }
        }

        hospitalRepository.deleteHospital(id);
        return "redirect:/superadmin/hospital-list";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
