package com.grppj.donateblood.controller;

import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class BloodStockController {

    private final DonorRepository donorRepository;
    private final HospitalRepository hospitalRepository;

    public BloodStockController(DonorRepository donorRepository,
                                HospitalRepository hospitalRepository) {
        this.donorRepository = donorRepository;
        this.hospitalRepository = hospitalRepository;
    }

    /** /admin/bloodstock?hid=1 */
    @GetMapping("/bloodstock")
    public String stockQuery(@RequestParam(name = "hid", defaultValue = "1") int hid, Model model) {
        return populate(hid, model);
    }

    /** /admin/bloodstock/1 */
    @GetMapping("/bloodstock/{hid}")
    public String stockPath(@PathVariable int hid, Model model) {
        return populate(hid, model);
    }

    private String populate(int hid, Model model) {
        // sidebar/topnav bits
        model.addAttribute("active", "blood-stock"); // or "analytics" if that menu item exists
        model.addAttribute("userName", "Admin");
        model.addAttribute("avatarUrl", null);

        // data for the page
        model.addAttribute("hid", hid);
        model.addAttribute("rows", donorRepository.getStockFromDonationsByHospital(hid));

        String hospitalName = hospitalRepository.findNameById(hid);
        model.addAttribute("hospitalName", hospitalName != null ? hospitalName : "Unknown Hospital");

        // your template
        return "admin/admin-bloodstock";
    }
}
