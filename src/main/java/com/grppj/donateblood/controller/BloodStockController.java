// src/main/java/com/grppj/donateblood/controller/BloodStockController.java
package com.grppj.donateblood.controller;

import com.grppj.donateblood.repository.BloodUsageRepository;
import com.grppj.donateblood.repository.DonorRepository;
import com.grppj.donateblood.repository.HospitalRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class BloodStockController {

    private final DonorRepository donorRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodUsageRepository bloodUsageRepository;

    public BloodStockController(DonorRepository donorRepository,
                                HospitalRepository hospitalRepository,
                                BloodUsageRepository bloodUsageRepository) {
        this.donorRepository = donorRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodUsageRepository = bloodUsageRepository;
    }

    /* ---------- helpers ---------- */
    private Integer resolveHospitalId(HttpSession session, Integer hidParam) {
        if (hidParam != null) return hidParam;
        Object v = (session == null) ? null : session.getAttribute("HOSPITAL_ID");
        if (v instanceof Integer) return (Integer) v;
        if (v != null) { try { return Integer.valueOf(String.valueOf(v)); } catch (NumberFormatException ignore) {} }
        return hospitalRepository.findAll().stream().findFirst().map(h -> h.getId()).orElse(null);
    }
    private String resolveHospitalName(HttpSession session, Integer hid) {
        Object cachedName = (session == null) ? null : session.getAttribute("HOSPITAL_NAME");
        if (cachedName instanceof String && hid != null) return (String) cachedName;
        String db = (hid == null) ? null : hospitalRepository.findNameById(hid);
        return (db != null && !db.isBlank()) ? db : "Unknown Hospital";
    }

    private String populate(Integer hid, Model model, HttpSession session) {
        model.addAttribute("active", "blood-stock");
        model.addAttribute("userName", (session != null && session.getAttribute("ADMIN_NAME") != null)
                ? session.getAttribute("ADMIN_NAME") : "Admin");
        model.addAttribute("avatarUrl", (session != null) ? session.getAttribute("ADMIN_AVATAR_URL") : null);
        model.addAttribute("hid", hid);
        model.addAttribute("rows",
                (hid == null) ? java.util.List.of()
                        : donorRepository.getStockFromDonationsByHospital(hid));
        model.addAttribute("hospitalName", resolveHospitalName(session, hid));
        return "admin/admin-bloodstock";   // <- template: templates/admin/admin-bloodstock.html
    }

    /* ---------- GET: stock page ---------- */
    @GetMapping("/bloodstock")
    public String stockDefault(Model model, HttpSession session) {
        Integer hid = resolveHospitalId(session, null);
        return populate(hid, model, session);
    }

    @GetMapping(value = "/bloodstock", params = "hid")
    public String stockQuery(@RequestParam("hid") Integer hid, Model model, HttpSession session) {
        return populate(resolveHospitalId(session, hid), model, session);
    }

    @GetMapping("/bloodstock/{hid}")
    public String stockPath(@PathVariable Integer hid, Model model, HttpSession session) {
        return populate(resolveHospitalId(session, hid), model, session);
    }

    /* ---------- GET: usage history page ---------- */
    /* ---------- USAGE HISTORY (READ) ---------- */
    @GetMapping("/blood-stock/usage")
    public String usagePage(Model model, HttpSession session) {
        Integer hid = sessionHospitalId(session);
        if (hid == null) {
            session.setAttribute("error", "No hospital assigned to your account.");
            return "redirect:/login";
        }

        model.addAttribute("active", "blood-usage");
        model.addAttribute("userName",
                session.getAttribute("ADMIN_NAME") != null ? session.getAttribute("ADMIN_NAME") : "Admin");
        model.addAttribute("avatarUrl", session.getAttribute("ADMIN_AVATAR_URL"));
        model.addAttribute("hospitalName",
                session.getAttribute("HOSPITAL_NAME") != null
                        ? session.getAttribute("HOSPITAL_NAME")
                        : hospitalRepository.findNameById(hid));

        // Strictly scoped to THIS admin’s hospital
        model.addAttribute("rows", bloodUsageRepository.findByHospital(hid));
        return "admin/admin-blood-usage";
    }


    /* ---------- POST: use blood with remarks ---------- */
    /* ---------- USE BLOOD (WRITE) ---------- */
    @PostMapping("/blood-stock/use")
    public String useBloodUnits(@RequestParam("bloodTypeId") int bloodTypeId,
                                @RequestParam("usedUnits") int usedUnits,
                                @RequestParam(value = "remarks", required = false) String remarks,
                                HttpSession session) {

        Integer hid = sessionHospitalId(session);
        if (hid == null || usedUnits <= 0) {
            session.setAttribute("error", "Invalid request or no hospital assigned.");
            return "redirect:/admin/bloodstock";
        }

        // admin identity
        var loginUser = session.getAttribute("loginuser");
        int adminId = (loginUser instanceof com.grppj.donateblood.model.User u) ? u.getId() : 0;
        int adminRoleId = (loginUser instanceof com.grppj.donateblood.model.User u)
                ? (u.getRole() != null ? u.getRole().getId() : 2) : 2; // default 2=admin

        // decrease units driven by hospital in session
        var result = donorRepository.useBloodUnitsDetailed(bloodTypeId, hid, usedUnits);
        if (result.actualUsed <= 0) {
            session.setAttribute("error", "Blood units not available to use.");
            return "redirect:/admin/bloodstock";
        }

        // log usage strictly for this hospital
        int usageId = bloodUsageRepository.insertUsage(hid, bloodTypeId, result.actualUsed, adminId, adminRoleId, remarks);
        bloodUsageRepository.insertUsageDetails(usageId, result.donationIds);

        session.setAttribute("success", result.actualUsed + " unit(s) successfully used.");
        return "redirect:/admin/blood-stock/usage";
    }
    private Integer sessionHospitalId(HttpSession session) {
        if (session == null) return null;
        Object v = session.getAttribute("HOSPITAL_ID");
        if (v instanceof Integer) return (Integer) v;
        if (v != null) {
            try { return Integer.valueOf(String.valueOf(v)); } catch (Exception ignore) {}
        }
        return null;
    }
}
    
