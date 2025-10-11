package com.grppj.donateblood.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;   // <-- important
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.grppj.donateblood.model.BloodRequestBean;
import com.grppj.donateblood.repository.BloodRequestRepository;
import com.grppj.donateblood.repository.BloodTypeRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.BloodStockRepository;

@Controller
@RequestMapping("/admin")
public class BloodRequestAdminController {

    @Autowired private BloodRequestRepository requestRepo;
    @Autowired private BloodTypeRepository bloodTypeRepo;
    @Autowired private HospitalRepository hospitalRepo;

    // we call repos directly here (no service)
    @Autowired private DonorRepository donorRepo;
    @Autowired private BloodStockRepository stockRepo;

    // List pending requests for one hospital (example: default to first hospital)
    @GetMapping("/requests")
    public String list(Model model,
            @RequestParam(value="hospitalId", required=false) Integer hospitalId) {

        int hid = (hospitalId != null) ? hospitalId : hospitalRepo.getAllHospitals().get(0).getId();
        model.addAttribute("hospitalId", hid);
        model.addAttribute("pending", requestRepo.listPendingByHospital(hid));
        return "admin/admin-requests";
    }

    // Show create-form
    @GetMapping("/requests/add")
    public String addForm(Model model,
            @RequestParam(value="hospitalId", required=false) Integer hospitalId) {
        int hid = (hospitalId != null) ? hospitalId : hospitalRepo.getAllHospitals().get(0).getId();
        model.addAttribute("hospitalId", hid);
        model.addAttribute("bloodTypes", bloodTypeRepo.getAllBloodTypes());
        model.addAttribute("req", new BloodRequestBean());
        return "admin/admin-request-add";
    }

    // Create request
    @PostMapping("/requests/add")
    public String add(@ModelAttribute("req") BloodRequestBean req) {
        if (req.getHospitalId() == null) {
            req.setHospitalId(hospitalRepo.getAllHospitals().get(0).getId());
        }
        // plug your real requester here
        req.setUserId(1);
        req.setUserRoleId(3); // e.g., 3 = recipient
        requestRepo.create(req);
        return "redirect:/admin/requests?hospitalId=" + req.getHospitalId();
    }

    // Fulfill request (no service). Everything in one transaction.
    @PostMapping("/requests/{id}/fulfill")
    @Transactional
    public String fulfill(@PathVariable int id,
            @RequestParam(value="hospitalId", required=false) Integer hospitalId) {

        Map<String,Object> req = requestRepo.findById(id);
        if (req == null) {
            return "redirect:/admin/requests" + (hospitalId!=null?("?hospitalId="+hospitalId):"");
        }

        int hid  = ((Number) req.get("hospital_id")).intValue();
        int btId = ((Number) req.get("blood_type_id")).intValue();
        int qty  = ((Number) req.get("quantity")).intValue();

        // your logged-in admin identity here
        int adminId = 1, adminRoleId = 1;

        // pick oldest available donations up to qty
        List<Map<String,Object>> donations =
                donorRepo.findAvailableDonationsForHospital(hid, btId, qty);

        int allocated = 0;
        for (Map<String,Object> d : donations) {
            int donationId = ((Number) d.get("donation_id")).intValue();

            // flip Available -> Used (idempotent)
            int changed = donorRepo.markDonationUsed(donationId);
            if (changed > 0) {
                // subtract stock and record linkage
                stockRepo.decreaseStock(hid, btId, 1, adminId, adminRoleId);
                requestRepo.insertFulfillmentRow(donationId, id, 1);
                allocated++;
                if (allocated == qty) break;
            }
        }

        if (allocated == qty) {
            requestRepo.markFulfilled(id);
        }
        // if not enough units, request stays 'pending'

        return "redirect:/admin/requests" + (hospitalId!=null?("?hospitalId="+hospitalId):"");
    }
}
