package com.grppj.donateblood.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.grppj.donateblood.model.Appointment;
import com.grppj.donateblood.model.BloodRequest;
import com.grppj.donateblood.model.BloodType;
import com.grppj.donateblood.model.DonationBean;
import com.grppj.donateblood.model.Hospital;
import com.grppj.donateblood.model.Urgency;
import com.grppj.donateblood.model.User;
import com.grppj.donateblood.model.UserMessageBean;
import com.grppj.donateblood.repository.AppointmentRepository;
import com.grppj.donateblood.repository.BloodRequestRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.DonationRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.repository.UserMessageRepository;
import com.grppj.donateblood.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


@Controller
public class ProfileController {

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private BloodRequestRepository bloodRequestRepo;
   
    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private HospitalRepository hospitalRepo;
    
    @Autowired
    private BloodTypeRepository bloodTypeRepo;
    
    @Autowired
    private DonationRepository donationRepo;
    
    @Autowired
    private UserMessageRepository userMessageRepo;


    // Profile main view
    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        model.addAttribute("user", loginUser);

        // If donor, load appointments
        if ("donor".equalsIgnoreCase(loginUser.getRole().getRole())) {
            List<Appointment> appointments = appointmentRepo.findByUserId(loginUser.getId());
            model.addAttribute("appointments", appointments);
        }

        // If receiver, load blood requests
        if ("receiver".equalsIgnoreCase(loginUser.getRole().getRole())) {
            List<BloodRequest> requests = bloodRequestRepo.findByUserId(loginUser.getId());
            model.addAttribute("requests", requests);
        }

        return "profile";
    }
    

    @GetMapping("/user/{id}/image")
    @ResponseBody
    public ResponseEntity<byte[]> getUserImage(@PathVariable int id) {
        User user = userRepo.getUserById(id);

        if (user != null && user.getImageBytes() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG); // or detect PNG
            return new ResponseEntity<>(user.getImageBytes(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Donor: Appointment fragment
    @GetMapping("/profile/my-appointments")
    public String myAppointmentsFragment(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        List<Appointment> appointments = appointmentRepo.findByUserId(loginUser.getId());
        model.addAttribute("appointments", appointments);
        return "fragments/myAppointments :: myAppointmentsTable";
    }

    @GetMapping("/profile/blood-request")
    public String myBloodRequestsFragment(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        List<BloodRequest> requests = bloodRequestRepo.findByUserId(loginUser.getId());
        model.addAttribute("requests", requests);

        return "fragments/bloodRequests :: myBloodRequestsTable";
    }



    @GetMapping("/profile/donated-history")
    public String donatedHistory(HttpSession session, Model model) {
    	User loginUser = (User) session.getAttribute("loginuser");
    	if (loginUser == null) return "redirect:/login";

    	List<DonationBean> donations = donationRepo.findByUserId(loginUser.getId());
    	model.addAttribute("donations", donations);
    	return "fragments/donatedHistory :: donatedHistory";
    }
   
    // Messages
    @GetMapping("/profile/messages")
    public String messages(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        List<UserMessageBean> messages = userMessageRepo.getMessagesByReceiver(loginUser.getId());
        model.addAttribute("messages", messages);
        return "fragments/messages :: messagesSection";
    }

    @PostMapping("/profile/mark-message-read")
    @ResponseBody
    public String markMessageAsRead(@RequestParam("msgId") int msgId) {
        userMessageRepo.markAsRead(msgId);
        return "success";
    }



    // Edit Profile
    @GetMapping("/profile/edit-profile")
    public String editProfile(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        User user = userRepo.getUserById(loginUser.getId());
        String bloodTypeName = "Unknown";
        if (user.getBloodTypeId() != null) {
            BloodType bt = bloodTypeRepo.getBloodTypeById(user.getBloodTypeId());
            if (bt != null) {
                bloodTypeName = bt.getBloodType();
            }
        }
        model.addAttribute("user", user);
        model.addAttribute("bloodTypeName", bloodTypeName);
        return "fragments/editProfile :: editProfileForm";
    }

    @PostMapping("/profile/update")
    @ResponseBody
    public String updateProfile(User updatedUser, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";
              

        try {
            // ✅ Update normal user fields
            loginUser.setUsername(updatedUser.getUsername());
            loginUser.setEmail(updatedUser.getEmail());
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            	loginUser.setPassword(updatedUser.getPassword());
            }

            loginUser.setAddress(updatedUser.getAddress());
            loginUser.setPhone(updatedUser.getPhone());

            // ✅ Handle profile image upload
            if (updatedUser.getFilePart() != null && !updatedUser.getFilePart().isEmpty()) {
                loginUser.setImageBytes(updatedUser.getFilePart().getBytes());
            }

            // ✅ Save to DB
            userRepo.updateUser(loginUser);

            // ✅ Update session
            session.setAttribute("loginuser", loginUser);

            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "fail";
        }
    }

    
    @PostMapping("/profile/update-image")
    @ResponseBody
    public String updateProfileImage(@RequestParam("filePart") MultipartFile filePart, HttpSession session) {
        try {
            User loginUser = (User) session.getAttribute("loginuser");
            if (loginUser == null) return "fail";

            if (filePart != null && !filePart.isEmpty()) {
                byte[] imageBytes = filePart.getBytes();
                int updated = userRepo.updateUserImage(loginUser.getId(), imageBytes);
                if (updated == 1) {
                    loginUser.setImageBytes(imageBytes);
                    session.setAttribute("loginuser", loginUser);
                    return "success";
                }
            }
            return "fail";
        } catch (Exception e) {
            e.printStackTrace();
            return "fail";
        }
    }



    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
    
    
    @GetMapping("/profile/edit-appointment/{id}")
    public String editAppointmentForm(@PathVariable Integer id, Model model) {
        Appointment appointment = appointmentRepo.findById(id);
        model.addAttribute("appointment", appointment);

        // Get all hospitals from database
        List<Hospital> hospitals = hospitalRepo.findAll(); 
        model.addAttribute("hospitals", hospitals);

        // Get blood type name for display (read-only)
        String bloodTypeName = "Not set";
        if (appointment.getBloodTypeId() != null) {
            BloodType bloodType = bloodTypeRepo.getBloodTypeById(appointment.getBloodTypeId());
            if (bloodType != null) {
                bloodTypeName = bloodType.getBloodType();
            }
        }
        model.addAttribute("bloodTypeName", bloodTypeName);

        return "fragments/editAppointmentForm :: editAppointmentFormFragment";
    }



    @PostMapping("/profile/update-appointment")
    @ResponseBody
    public String updateAppointment(
            @RequestParam Integer id,
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam Integer bloodTypeId,
            @RequestParam Integer hospitalId,
            HttpSession session) {

        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        Appointment existing = appointmentRepo.findById(id);
        if (existing == null || existing.getUserId() != loginUser.getId()) {
            return "fail";
        }

        try {
            existing.setDate(LocalDate.parse(date));
            existing.setTime(time);
            existing.setBloodTypeId(bloodTypeId);
            existing.setHospitalId(hospitalId);

            int updated = appointmentRepo.update(existing);
            return updated == 1 ? "success" : "fail";
        } catch (Exception e) {
            e.printStackTrace();
            return "fail";
        }
    }


    
 // Edit Blood Request Form (like editAppointmentForm)
    @GetMapping("/profile/edit-blood-request/{id}")
    public String editBloodRequestForm(@PathVariable int id, Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        BloodRequest request = bloodRequestRepo.findById(id);
        if (request == null || !request.getUserId().equals(loginUser.getId())) {
            return "redirect:/profile/blood-request";
        }

        model.addAttribute("bloodRequest", request);

        // Get all hospitals
        List<Hospital> hospitals = hospitalRepo.findAll();
        model.addAttribute("hospitals", hospitals);

        // Get all blood types
        List<BloodType> bloodTypes = bloodTypeRepo.findAll();
        model.addAttribute("bloodTypes", bloodTypes);

        // Get urgency options
        model.addAttribute("urgencies", Urgency.values());

        return "fragments/editBloodRequestForm :: editBloodRequestFormFragment";
    }

    // Update Blood Request (like updateAppointment)
    @PostMapping("/profile/update-blood-request")
    @ResponseBody
    public String updateBloodRequest(
            @RequestParam int id,
            @RequestParam int bloodTypeId,
            @RequestParam int hospitalId,
            @RequestParam int quantity,
            @RequestParam String requiredDate,
            @RequestParam String urgency,
            HttpSession session) {

        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        BloodRequest existing = bloodRequestRepo.findById(id);
        if (existing == null || !existing.getUserId().equals(loginUser.getId())) {
            return "fail";
        }

        try {
            existing.setBloodTypeId(bloodTypeId);
            existing.setHospitalId(hospitalId);
            existing.setQuantity(quantity);
            existing.setRequiredDate(LocalDate.parse(requiredDate));
            existing.setUrgency(Urgency.valueOf(urgency));

            int updated = bloodRequestRepo.update(existing);
            return updated == 1 ? "success" : "fail";
        } catch (Exception e) {
            e.printStackTrace();
            return "fail";
        }
    }
    
    @GetMapping("/profile/delete-appointment/{id}")
    public String deleteAppointment(@PathVariable Integer id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        Appointment existing = appointmentRepo.findById(id);
        if (existing != null && existing.getUserId().equals(loginUser.getId())) {
            appointmentRepo.deleteById(id);
        }

        // Redirect back to profile page (or appointments fragment)
        return "redirect:/profile";
    }

    @GetMapping("/profile/delete-blood-request/{id}")
    public String deleteRequest(@PathVariable Integer id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/login";

        BloodRequest existing = bloodRequestRepo.findById(id);
        if (existing != null && existing.getUserId().equals(loginUser.getId())) {
        	bloodRequestRepo.deleteById(id);
        }

        // Redirect back to profile page (or appointments fragment)
        return "redirect:/profile";
    }
    
    @GetMapping("/profile/certificate/{id}")
    public String viewCertificate(@PathVariable Integer id, HttpSession session, Model model) {
        // Check if user is logged in
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // Fetch donation by ID
        DonationBean donation = donationRepo.findDonationInfoById(id);
        if (donation == null) {
            return "redirect:/profile/donated-history";
        }

        

        // Add data to model
        model.addAttribute("donation", donation);
        model.addAttribute("donorName", loginUser.getUsername());
        model.addAttribute("hospitalName", donation.getHospitalName()); // use hospital from DonationBean
        model.addAttribute("donationDate", donation.getDonationDate());

        return "fragments/certificate"; // Thymeleaf template
    }

}
