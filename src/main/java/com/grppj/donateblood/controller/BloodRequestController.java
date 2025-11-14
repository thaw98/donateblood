package com.grppj.donateblood.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.grppj.donateblood.model.BloodRequest;
import com.grppj.donateblood.model.User;
import com.grppj.donateblood.model.AppointmentStatus;
import com.grppj.donateblood.repository.BloodRequestRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.HospitalRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class BloodRequestController {

    @Autowired
    private BloodRequestRepository bloodRequestRepo;

    @Autowired
    private HospitalRepository hospitalRepo;

    @Autowired
    private BloodTypeRepository bloodTypeRepo;

    @GetMapping("/bloodrequest/form")
    public String showBloodRequestForm(Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/loginform";

        BloodRequest reqObj = new BloodRequest();
        reqObj.setStatus(AppointmentStatus.pending);
        reqObj.setUserId(loginUser.getId());

        model.addAttribute("reqObj", reqObj);
        model.addAttribute("hospitalList", hospitalRepo.findAll());
        model.addAttribute("bloodList", bloodTypeRepo.findAll());

        return "bloodrequest";
    }

    @PostMapping("/bloodrequest/save")
    public String saveBloodRequest(@ModelAttribute("reqObj") BloodRequest reqObj, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginuser");
        if (loginUser == null) return "redirect:/loginform";

        reqObj.setUserId(loginUser.getId());
        if (reqObj.getStatus() == null) reqObj.setStatus(AppointmentStatus.pending);

        bloodRequestRepo.saveBloodRequest(reqObj);
        return "redirect:/indexR";
    }

}
