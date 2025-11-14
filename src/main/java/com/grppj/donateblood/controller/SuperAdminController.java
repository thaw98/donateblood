package com.grppj.donateblood.controller;

import com.grppj.donateblood.model.User;
import com.grppj.donateblood.model.*;
import com.grppj.donateblood.model.Hospital;
import com.grppj.donateblood.repository.UserRepository;
import com.grppj.donateblood.repository.BloodStockRepository;
import com.grppj.donateblood.repository.DashboardRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Random;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {
	@Autowired
	private DashboardRepository dashboardRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;
   
    @Autowired
    private BloodStockRepository bloodStockRepo; // Autowire the blood stock repo

    @Autowired
    private BloodTypeRepository bloodTypeRepo;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;



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

   
 // In SuperAdminController.java

    
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "hospitalId", required = false) Integer hospitalId,
                            @RequestParam(value = "donorPage", defaultValue = "0") int donorPage,
                            @RequestParam(value = "receiverPage", defaultValue = "0") int receiverPage,
                            HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        User superadmin = (User) session.getAttribute("superadmin");
        List<User> admins = userRepository.findAllAdmins();
        List<Hospital> hospitals = hospitalRepository.findAllHospitals();
        long totalUsers = userRepository.countAllUsers();
        
        // --- Blood Stock Logic ---
        List<BloodStockRepository.StockViewRow> displayStockList = new ArrayList<>();
        if (hospitalId != null) {
            displayStockList = bloodStockRepo.getStockViewForSuperAdminDashboard(hospitalId);
        }
        model.addAttribute("displayStockList", displayStockList);
        
        // --- User Records Logic - Split into Donors and Receivers ---
        List<UserAppointmentView> allUsers = new ArrayList<>();
        try {
            if (hospitalId != null) {
                allUsers = userRepository.findUserAppointmentsByHospitalId(hospitalId);
            } else {
                allUsers = userRepository.findAllUserAppointments();
            }
        } catch (Exception e) {
            e.printStackTrace();
            allUsers = new ArrayList<>();
        }

        // Separate donors and receivers
        List<UserAppointmentView> allDonors = allUsers.stream()
            .filter(user -> "DONOR".equals(user.getUserType()))
            .collect(Collectors.toList());
        
        List<UserAppointmentView> allReceivers = allUsers.stream()
            .filter(user -> "RECEIVER".equals(user.getUserType()))
            .collect(Collectors.toList());
        
        // Paginate donors (5 per page)
        int donorPageSize = 5;
        int donorTotalPages = Math.max(1, (int) Math.ceil((double) allDonors.size() / donorPageSize));
        donorPage = Math.max(0, Math.min(donorPage, donorTotalPages - 1));
        List<UserAppointmentView> paginatedDonors = allDonors.stream()
            .skip(donorPage * donorPageSize)
            .limit(donorPageSize)
            .collect(Collectors.toList());
        
        // Paginate receivers (5 per page)
        int receiverPageSize = 5;
        int receiverTotalPages = Math.max(1, (int) Math.ceil((double) allReceivers.size() / receiverPageSize));
        receiverPage = Math.max(0, Math.min(receiverPage, receiverTotalPages - 1));
        List<UserAppointmentView> paginatedReceivers = allReceivers.stream()
            .skip(receiverPage * receiverPageSize)
            .limit(receiverPageSize)
            .collect(Collectors.toList());

        model.addAttribute("admins", admins);
        model.addAttribute("hospitals", hospitals);
        model.addAttribute("superadmin", superadmin);
        model.addAttribute("selectedHospitalId", hospitalId);
        model.addAttribute("totalUsers", totalUsers);
        
        // Add donor/receiver data for tabs
        model.addAttribute("donors", paginatedDonors);
        model.addAttribute("receivers", paginatedReceivers);
        model.addAttribute("currentDonorPage", donorPage);
        model.addAttribute("currentReceiverPage", receiverPage);
        model.addAttribute("donorTotalPages", donorTotalPages);
        model.addAttribute("receiverTotalPages", receiverTotalPages);
        model.addAttribute("totalDonors", allDonors.size());
        model.addAttribute("totalReceivers", allReceivers.size());

        return "superadmin/dashboard";
    }
    
    
    // =================== PROFILE MANAGEMENT ===================

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User superadmin = (User) session.getAttribute("superadmin");
        if (superadmin == null) {
            return "redirect:/login";
        }

        User fullProfile = userRepository.getUserById(superadmin.getId());
        model.addAttribute("avatarUrl", "/superadmin/user/" + fullProfile.getId() + "/image");
        model.addAttribute("user", fullProfile);
        return "superadmin/profile";
    }

 // Serve profile image
    @GetMapping("/user/{id}/image")
    public ResponseEntity<byte[]> getUserImage(@PathVariable int id) {
        User user = userRepository.getUserById(id);
        if (user != null && user.getImageBytes() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(user.getImageBytes(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Update profile image via AJAX
    @PostMapping("/profile/update-image")
    @ResponseBody
    public String updateProfileImage(@RequestParam("filePart") MultipartFile filePart, HttpSession session) {
        try {
            User superadmin = (User) session.getAttribute("superadmin");
            if (superadmin == null) return "fail";

            if (filePart != null && !filePart.isEmpty()) {
                byte[] imageBytes = filePart.getBytes();
                int updated = userRepository.updateUserImage(superadmin.getId(), imageBytes);

                if (updated == 1) {
                    superadmin.setImageBytes(imageBytes);
                    session.setAttribute("superadmin", superadmin);
                    return "success";
                }
            }
            return "fail";
        } catch (Exception e) {
            e.printStackTrace();
            return "fail";
        }
    }

    // Update profile info (username, email, phone, address, password)
    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("address") String address,
            @RequestParam("phone") String phone,
            @RequestParam(value = "password", required = false) String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User superadmin = (User) session.getAttribute("superadmin");
        if (superadmin == null) return "redirect:/login";

        User currentUser = userRepository.getUserById(superadmin.getId());
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/superadmin/profile";
        }

        currentUser.setUsername(username);
        currentUser.setEmail(email);
        currentUser.setAddress(address);
        currentUser.setPhone(phone);

        if (password != null && !password.trim().isEmpty()) {
            currentUser.setPassword(password);
        }

        int result = userRepository.updateUser(currentUser);

        if (result > 0) {
            session.setAttribute("superadmin", currentUser);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to update profile.");
        }

        return "redirect:/superadmin/profile";
    }


    // ================= CREATE ADMIN WITH OTP =================
    @GetMapping("/create-admin")
    public String createAdminForm(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        List<Hospital> hospitals = hospitalRepository.findAllHospitals();
        List<User> admins = userRepository.findAllAdmins();
        model.addAttribute("bloodTypes", bloodTypeRepo.findAll());
        
        model.addAttribute("hospitals", hospitals);
        model.addAttribute("admins", admins);
        return "superadmin/create-admin";
    }

    @PostMapping("/create-admin")
    public String createAdmin(@RequestParam String email,
                              @RequestParam String gender,
                              @RequestParam String dateOfBirth,
                              @RequestParam String address,
                              @RequestParam int blood_type_id,
                              @RequestParam String phone,
                              @RequestParam(required = false) Integer hospitalId,
                              HttpSession session,
                              Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        try {
            // Auto-generate username from email
            String username = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "_");

            // ✅ Step 1: Check email format
            if (!isValidEmailFormat(email)) {
                model.addAttribute("error", "❌ Invalid email format: " + email);
                return reloadCreateAdminData(model);
            }

            // ✅ Step 2: Check if domain is real
            if (!isValidDomain(email)) {
                model.addAttribute("error", "❌ No matches for '" + email + "'");
                return reloadCreateAdminData(model);
            }
            
            // Check if email already exists
            if (userRepository.emailExists(email)) {
                model.addAttribute("error", "Email already exists: " + email);
                return reloadCreateAdminData(model);
            }
            
            if (!phone.matches("09[0-9]{7,11}")) {
                model.addAttribute("error", "❌ Invalid phone number format. Must start with 09 and be 9–13 digits long.");
                return reloadCreateAdminData(model);
            }

            // Generate OTP
            String verificationCode = generateVerificationCode();
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

            // Save admin with OTP
            boolean created = userRepository.createAdmin(username, email, gender, dateOfBirth, address,
                    blood_type_id, phone, hospitalId, verificationCode);

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
            emailService.sendVerificationCode(email, verificationCode);

            model.addAttribute("success",
                    "✅ Admin created successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error: " + e.getMessage());
        }

        return reloadCreateAdminData(model);
    }

    private String reloadCreateAdminData(Model model) {
        List<Hospital> hospitals = hospitalRepository.findAllHospitals();
        List<User> admins = userRepository.findAllAdmins(); // <-- ADD THIS LINE
        
        model.addAttribute("hospitals", hospitals);
        model.addAttribute("admins", admins);               // <-- ADD THIS LINE
        return "superadmin/create-admin";
    }
    
 // ✅ Add these helper methods at the bottom of your controller
    private boolean isValidEmailFormat(String email) {
        String regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email != null && email.matches(regex);
    }
    
    private boolean isValidDomain(String email) {
        try {
            String domain = email.substring(email.indexOf("@") + 1);
            javax.naming.directory.InitialDirContext ctx =
                    new javax.naming.directory.InitialDirContext();
            javax.naming.directory.Attributes attrs =
                    ctx.getAttributes("dns:/" + domain, new String[]{"MX"});
            return attrs != null && attrs.size() > 0;
        } catch (Exception e) {
            return false;
        }
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
        
        // Create a map for easy lookup of hospital names by ID
        Map<Integer, String> hospitalNameMap = hospitals.stream()
                .collect(Collectors.toMap(Hospital::getId, Hospital::getHospitalName));

        // Enrich each admin object with its hospital name
        for (User admin : admins) {
            if (admin.getHospitalId() != null) {
                admin.setHospitalName(hospitalNameMap.get(admin.getHospitalId()));
            }
        }
        
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
 // In SuperAdminController.java

    @PostMapping("/unassign-admin") // <-- Changed to @PostMapping
    public String unassignAdminFromHospital(@RequestParam("adminId") int adminId, // <-- Changed to @RequestParam
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        if (session.getAttribute("superadmin") == null) {
            return "redirect:/login";
        }

        try {
            // Get admin details for the success message
            User admin = userRepository.getUserById(adminId);
            
            boolean success = userRepository.unassignAdminFromHospital(adminId);

            if (success && admin != null) {
                redirectAttributes.addFlashAttribute("success", 
                    "✅ Admin '" + admin.getUsername() + "' has been successfully unassigned.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to unassign admin. They may not have been assigned to a hospital.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "An error occurred while unassigning the admin.");
        }

        return "redirect:/superadmin/admin-list";
    }

    
    @GetMapping("/add-hospital")
    public String addHospitalForm(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        model.addAttribute("hospital", new Hospital());
        List<Hospital> hospitals = hospitalRepository.findAllHospitals();
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
                        String extension = originalFileName != null && originalFileName.contains(".")
                                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                                : "";
                        String uniqueFileName = UUID.randomUUID() + extension;

                        Path filePath = Paths.get(UPLOAD_DIR, uniqueFileName);
                        Files.copy(file.getInputStream(), filePath);

                        uploadedFileNames.add(uniqueFileName);
                    }
                }
            }

            // **Store as comma-separated string**
            hospital.setProfilePicture(String.join(",", uploadedFileNames));

            boolean success = hospitalRepository.addHospital(hospital);

            if (success) {
                model.addAttribute("success", "Hospital added successfully with " + uploadedFileNames.size() + " profile pictures!");
            } else {
                model.addAttribute("error", "Failed to add hospital!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "An error occurred: " + e.getMessage());
        }

        model.addAttribute("hospital", new Hospital());
        return "superadmin/add-hospital";
    }

    
    
    @GetMapping("/hospital-profile/{id}")
    public String viewHospitalProfile(@PathVariable Integer id, HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        Hospital hospital = hospitalRepository.findById(id);
        List<User> hospitalAdmins = userRepository.findAdminsByHospital(id);

        model.addAttribute("hospital", hospital);        // hospital.getProfilePictures() works in view
        model.addAttribute("admins", hospitalAdmins);
        return "superadmin/hospital-profile";
    }

    // =================== REMAINING CONTROLLER LOGIC (unchanged) ===================

    @GetMapping("/admin-list")
    public String adminList(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        // Fetch all admins
        List<User> admins = userRepository.findAllAdmin();
        
        // Calculate counts using the correct method
        long totalAdmins = admins.size();
        long assignedAdmins = admins.stream().filter(admin -> admin.getHospitalId() != null).count();
        long unassignedAdmins = admins.stream().filter(admin -> admin.getHospitalId() == null).count();
        long activeAdmins = admins.stream().filter(admin -> admin.isVerified()).count(); // This should work
        
        // Add to model
        model.addAttribute("admins", admins);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("assignedAdmins", assignedAdmins);
        model.addAttribute("unassignedAdmins", unassignedAdmins);
        model.addAttribute("activeAdmins", activeAdmins);
        model.addAttribute("hospitals", hospitalRepository.findAllHospitals());
        
        return "superadmin/admin-list";
    }
    
    @GetMapping("/superadmin/admin-detail")
    public String adminDetail(@RequestParam String email, Model model) {
        User admin = userRepository.findAdminByEmails(email);
        if (admin == null) {
            return "redirect:/superadmin/admin-list";
        }
        model.addAttribute("admin", admin);
        return "superadmin/admin-detail";
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
    
    @GetMapping("/activate-admin/{id}")
    public String activateAdmin(@PathVariable Integer id, HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        try {
            userRepository.updateUserVerificationStatus(id, true);
            model.addAttribute("success", "✅ Admin activated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to activate admin: " + e.getMessage());
        }
        
        return "redirect:/superadmin/admin-list";
    }

    @GetMapping("/deactivate-admin/{id}")
    public String deactivateAdmin(@PathVariable Integer id, HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        try {
            userRepository.updateUserVerificationStatus(id, false);
            model.addAttribute("success", "✅ Admin deactivated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Failed to deactivate admin: " + e.getMessage());
        }
        
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        hospitalRepository.deleteHospital(id);
        return "redirect:/superadmin/hospital-list";
    }
    @GetMapping("/admin-system-view")
    public String adminSystemView(HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        // Use the same data loading logic as AdminController
        model.addAttribute("totalBloodDonated", dashboardRepo.totalBloodDonatedUnits());
        model.addAttribute("completedDonors", dashboardRepo.completedDonors());
        model.addAttribute("pendingDonors", dashboardRepo.pendingDonors());
        model.addAttribute("totalAppointments", dashboardRepo.totalAppointments());
        
        // Super admin specific attributes
        model.addAttribute("active", "access-admin");
        model.addAttribute("userName", "Super Admin");
        model.addAttribute("avatarUrl", null);
        
        return "admin/dashboard"; // Reuse the same template
    }
    
    
    @GetMapping("/edit-hospital/{id}")
    public String editHospitalForm(@PathVariable("id") int id, HttpSession session, Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";
        
        Hospital hospital = hospitalRepository.findById(id);
        if (hospital == null) {
            return "redirect:/superadmin/hospital-list?error=Hospital not found";
        }
        model.addAttribute("hospital", hospital);
        
        // Add these attributes for the template fragments
        model.addAttribute("userName", "Super Admin");
        model.addAttribute("avatarUrl", null);
        
        return "superadmin/edit-hospital";
    }
    
    
    @PostMapping("/update-hospital/{id}")
    public String updateHospital(@PathVariable("id") int id,
                                @RequestParam String hospitalName,
                                @RequestParam String address,
                                @RequestParam String phoneNo,
                                @RequestParam String gmail,
                                @RequestParam(value = "profilePicturesFiles", required = false) MultipartFile[] profilePicturesFiles,
                                @RequestParam(value = "imagesToRemove", required = false) String[] imagesToRemove, // NEW: For removal
                                HttpSession session,
                                Model model) {
        if (session.getAttribute("superadmin") == null) return "redirect:/login";

        Hospital hospital = null;
        
        try {
            hospital = hospitalRepository.findById(id);
            if (hospital == null) {
                model.addAttribute("error", "Hospital not found");
                return "redirect:/superadmin/hospital-list";
            }

            // Update hospital fields
            hospital.setHospitalName(hospitalName);
            hospital.setAddress(address);
            hospital.setPhoneNo(phoneNo);
            hospital.setGmail(gmail);

            // STEP 1: Handle image removal FIRST
            List<String> currentFiles = new ArrayList<>();
            if (hospital.getProfilePicture() != null && !hospital.getProfilePicture().isEmpty()) {
                String[] existingFileArray = hospital.getProfilePicture().split(",");
                for (String file : existingFileArray) {
                    if (!file.trim().isEmpty()) {
                        currentFiles.add(file.trim());
                    }
                }
            }

            // Remove images that are checked for deletion
            if (imagesToRemove != null && imagesToRemove.length > 0) {
                for (String fileToRemove : imagesToRemove) {
                    currentFiles.remove(fileToRemove);
                    // Delete the physical file from server
                    try {
                        Files.deleteIfExists(Paths.get(UPLOAD_DIR, fileToRemove));
                        System.out.println("Removed file: " + fileToRemove);
                    } catch (IOException e) {
                        System.err.println("Failed to delete file: " + fileToRemove);
                        e.printStackTrace();
                    }
                }
            }

            // STEP 2: Handle new file uploads - APPEND to remaining files
            if (profilePicturesFiles != null && profilePicturesFiles.length > 0) {
                ensureUploadDirectoryExists();
                
                for (MultipartFile file : profilePicturesFiles) {
                    if (!file.isEmpty()) {
                        String originalFileName = file.getOriginalFilename();
                        String extension = originalFileName != null && originalFileName.contains(".")
                                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                                : "";
                        String uniqueFileName = UUID.randomUUID() + extension;
                        
                        Path filePath = Paths.get(UPLOAD_DIR, uniqueFileName);
                        Files.copy(file.getInputStream(), filePath);

                        currentFiles.add(uniqueFileName);
                    }
                }
            }

            // Update hospital with final list of images
            hospital.setProfilePicture(String.join(",", currentFiles));

            // STEP 3: Update database
            boolean success = hospitalRepository.updateHospital(id, hospital);

            if (success) {
                model.addAttribute("success", "Hospital updated successfully!");
            } else {
                model.addAttribute("error", "Failed to update hospital!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "An error occurred: " + e.getMessage());
        }

        // Always re-add hospital to model
        if (hospital == null) {
            hospital = hospitalRepository.findById(id);
        }
        model.addAttribute("hospital", hospital);
        model.addAttribute("userName", "Super Admin");
        model.addAttribute("avatarUrl", null);
        
        return "superadmin/edit-hospital";
    }



    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
