package com.grppj.donateblood.controller;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.grppj.donateblood.model.Appointment;
import com.grppj.donateblood.model.User;
import com.grppj.donateblood.model.AppointmentStatus;
import com.grppj.donateblood.repository.AppointmentRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.HospitalRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private HospitalRepository hospitalRepo;

    @Autowired
    private BloodTypeRepository bloodTypeRepo;

    @GetMapping("/appointment")
    public String showAppointmentForm(Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) {
            return "redirect:/loginform"; 
        }

        Appointment appointObj = new Appointment();
        appointObj.setStatus(AppointmentStatus.pending); // lowercase enum
        appointObj.setUserId(loginUser.getId());

        model.addAttribute("appointObj", appointObj);
        model.addAttribute("hospitals", hospitalRepo.findAll());
        model.addAttribute("bloodTypes", bloodTypeRepo.findAll());
        return "appointment"; 
    }

    @PostMapping("/appointment/save")
    public String saveAppointment(@ModelAttribute("appointObj") Appointment ap, 
                                  HttpSession session, 
                                  Model model,
                                  RedirectAttributes redirectAttrs) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) {
            return "redirect:/loginform";
        }
        
        LocalDate dob = loginUser.getDateofbirth();
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) {
            redirectAttrs.addFlashAttribute("errorMessage", 
                    "You must be at least 18 Years old to donate blood!");
            return "redirect:/appointment";
        }
        
        LocalDate lastDonationDate = appointmentRepo.findLastDonationDateByUserId(loginUser.getId());
        if(lastDonationDate != null) {
        	long daysSinceLast =ChronoUnit.DAYS.between(lastDonationDate, ap.getDate());
        	
        	if(daysSinceLast < 120 ) {
        		
        		redirectAttrs.addFlashAttribute("errorMessage",
        				"You cannot donate blood yet! Must wait at least 4 months since your last donation(" +
        		lastDonationDate + ").");
        		return "redirect:/appointment";
        	}
        }


        ap.setUserId(loginUser.getId());
        ap.setStatus(AppointmentStatus.pending); // lowercase enum
        appointmentRepo.doAppointment(ap);

        redirectAttrs.addFlashAttribute("successMessage", "Your appointment was successfully submitted!");
        return "redirect:/index";
       
    }
}
