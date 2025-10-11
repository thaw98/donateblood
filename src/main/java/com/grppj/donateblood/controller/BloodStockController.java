package com.grppj.donateblood.controller;

import com.grppj.donateblood.repository.BloodStockRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin") // -> all routes start with /admin
public class BloodStockController {

    @Autowired private BloodStockRepository bloodStockRepo;
    @Autowired private BloodTypeRepository bloodTypeRepo;

    // /admin/stock -> shows stock for Hospital ID = 1 only
 // StockController.java
    @GetMapping("/bloodstock")
    public String stock(Model model) {
        model.addAttribute("rows", bloodStockRepo.getViewForHospital(1)); // <- use the new method
        return "admin-bloodstock";
    }

}
