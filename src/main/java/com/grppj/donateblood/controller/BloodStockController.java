package com.grppj.donateblood.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.grppj.donateblood.repository.DonorRepository;

@Controller
@RequestMapping("/admin")
public class BloodStockController {

    @Autowired private DonorRepository donorRepository;

    @GetMapping("/bloodstock")
    public String stock(@RequestParam(name="hid", defaultValue="1") Integer hospitalId, Model model) {
        model.addAttribute("hid", hospitalId);
        model.addAttribute("rows", donorRepository.getStockFromDonationsByHospital(hospitalId));
        return "admin-bloodstock";
    }
}
