package com.grppj.donateblood.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.grppj.donateblood.repository.RecipientRepository;
import com.grppj.donateblood.repository.HospitalRepository;

@Controller
@RequestMapping("/admin")
public class RecipientAdminController {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

    private final RecipientRepository recipientRepository;
    private final HospitalRepository hospitalRepository;

    public RecipientAdminController(RecipientRepository recipientRepository,
                                    HospitalRepository hospitalRepository) {
        this.recipientRepository = recipientRepository;
        this.hospitalRepository = hospitalRepository;
    }

    @GetMapping("/recipients")
    public String recipients(Model model, HttpSession session) {

        Integer hospitalId = (Integer) session.getAttribute("HOSPITAL_ID");

        var rows = recipientRepository.listRecipientRequestsForHospital(hospitalId);

        var allHospitals = hospitalRepository.findAll();
        rows.forEach(row -> {
            allHospitals.stream()
                .filter(h -> h.getId() == row.getHospitalId())
                .findFirst()
                .ifPresent(h -> row.setHospitalName(h.getHospitalName()));
        });

        String hospitalTitle = (hospitalId == null)
                ? "All Hospitals"
                : allHospitals.stream()
                    .filter(h -> h.getId() == hospitalId)
                    .map(h -> h.getHospitalName())
                    .findFirst()
                    .orElse("Hospital " + hospitalId);

        model.addAttribute("rows", rows);
        model.addAttribute("hospitalTitle", hospitalTitle);
        model.addAttribute("hospitals", allHospitals);
        model.addAttribute("title", "Recipients");
        model.addAttribute("active", "recipients");
        
        // ✅ CHANGE: Use session attribute
        model.addAttribute("userName", 
                session.getAttribute("ADMIN_NAME") != null 
                    ? session.getAttribute("ADMIN_NAME") : "Admin");
        model.addAttribute("avatarUrl", session.getAttribute("ADMIN_AVATAR_URL"));
        
        return "admin/recipients";
    }


    /* 
     * Optional: back-end handlers (if you already have these elsewhere, keep yours)
     * They’re no-ops here — your existing business logic can remain.
     */

    /** Fulfill (Complete) a request. Uses session hospital if set, else the request's own hospital. */
    @PostMapping("/recipients/{id}/complete")
    public String completeRequest(@PathVariable("id") int requestId,
                                  @RequestParam("bloodTypeId") int bloodTypeId,
                                  @RequestParam("quantity") int quantity,
                                  HttpSession session) {

        Integer hospitalId = (Integer) session.getAttribute("HOSPITAL_ID");
        if (hospitalId == null) {
            hospitalId = recipientRepository.findHospitalIdForRequest(requestId);
        }

        if (hospitalId != null && quantity > 0) {
            int adminUserId = (session.getAttribute("USER_ID") instanceof Integer)
                    ? (Integer) session.getAttribute("USER_ID") : 0;

            // ✅ now updates status + inserts fulfillment rows
            recipientRepository.updateStatusAndInsertFulfillment(requestId, hospitalId, adminUserId, quantity);
        }

        return "redirect:/admin/recipients";
    }


    @PostMapping("/recipients/{id}/transfer")
    public String transferRequest(@PathVariable("id") int requestId,
                                  @RequestParam("targetHospitalId") int targetHospitalId,
                                  RedirectAttributes ra) {
        try {
            recipientRepository.transferAllUnitsNoTx(requestId, targetHospitalId);
            ra.addFlashAttribute("successMessage", "Request transferred to target hospital.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/recipients";
    }

}