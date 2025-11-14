// src/main/java/com/grppj/donateblood/controller/FulfillmentController.java
package com.grppj.donateblood.controller;

import com.grppj.donateblood.repository.FulfillmentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class FulfillmentController {

    @Autowired
    private FulfillmentRepository repo;

    @GetMapping("/fulfillment")
    public String fulfillmentList(Model model, HttpSession session) {
        model.addAttribute("active", "fulfillment");

        // ✅ CHANGE: USER_NAME → ADMIN_NAME
        Object name = session.getAttribute("ADMIN_NAME");
        model.addAttribute("userName", name != null ? name.toString() : "Admin");
        model.addAttribute("avatarUrl", session.getAttribute("ADMIN_AVATAR_URL"));

        Integer hospitalId = (Integer) session.getAttribute("HOSPITAL_ID");
        if (hospitalId != null) {
            model.addAttribute("rows", repo.findGroupedByHospital(hospitalId));
        } else {
            model.addAttribute("rows", repo.findGroupedAll());
        }
        return "admin/fulfillment";
    }


    // Show "who donated to this recipient request"
    @GetMapping("/requests/{id}/donors")
    public String donorsForRequest(@PathVariable("id") int requestId,
                                   Model model,
                                   HttpSession session) {
        model.addAttribute("active", "fulfillment");

        // ✅ CHANGE: USER_NAME → ADMIN_NAME
        Object name = session.getAttribute("ADMIN_NAME");
        model.addAttribute("userName", name != null ? name.toString() : "Admin");
        model.addAttribute("avatarUrl", session.getAttribute("ADMIN_AVATAR_URL"));

        var rows = repo.findDonorsForRequest(requestId);
        model.addAttribute("rows", rows);
        model.addAttribute("count", rows.size());

        String recipientName = repo.findRecipientNameForRequest(requestId);
        model.addAttribute("recipientName", recipientName);
        model.addAttribute("requestId", requestId);

        return "admin/request-donors";
    }
}
