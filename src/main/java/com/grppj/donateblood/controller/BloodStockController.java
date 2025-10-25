// src/main/java/com/grppj/donateblood/controller/BloodStockController.java
package com.grppj.donateblood.controller;

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

    public BloodStockController(DonorRepository donorRepository,
                                HospitalRepository hospitalRepository) {
        this.donorRepository = donorRepository;
        this.hospitalRepository = hospitalRepository;
    }

    /* ---------- helpers ---------- */
    private Integer resolveHospitalId(HttpSession session, Integer hidParam) {
        if (hidParam != null) return hidParam;

        Object v = (session == null) ? null : session.getAttribute("HOSPITAL_ID");
        if (v instanceof Integer) return (Integer) v;
        if (v != null) {
            try { return Integer.valueOf(String.valueOf(v)); } catch (NumberFormatException ignore) {}
        }
        // final fallback: first hospital in DB (keeps page usable if nothing in session)
        return hospitalRepository.findAll().stream().findFirst()
                .map(h -> h.getId()).orElse(null);
    }

    private String resolveHospitalName(HttpSession session, Integer hid) {
        // prefer name already on session, if it matches the current id
        Object cachedName = (session == null) ? null : session.getAttribute("HOSPITAL_NAME");
        if (cachedName instanceof String && hid != null) {
            return (String) cachedName; // good enough for the UI
        }
        // otherwise, read from DB
        String db = (hid == null) ? null : hospitalRepository.findNameById(hid);
        return (db != null && !db.isBlank()) ? db : "Unknown Hospital";
    }

    /* ---------- routes ---------- */

    /** GET /admin/bloodstock  → session hospital by default */
    @GetMapping("/bloodstock")
    public String stockDefault(Model model, HttpSession session) {
        Integer hid = resolveHospitalId(session, null);
        return populate(hid, model, session);
    }

    /** GET /admin/bloodstock?hid=2  (optional override by query param) */
    @GetMapping(value = "/bloodstock", params = "hid")
    public String stockQuery(@RequestParam("hid") Integer hid, Model model, HttpSession session) {
        return populate(resolveHospitalId(session, hid), model, session);
    }

    /** GET /admin/bloodstock/{hid}  (optional override by path) */
    @GetMapping("/bloodstock/{hid}")
    public String stockPath(@PathVariable Integer hid, Model model, HttpSession session) {
        return populate(resolveHospitalId(session, hid), model, session);
    }

    private String populate(Integer hid, Model model, HttpSession session) {
        // sidebar/topnav
        model.addAttribute("active", "blood-stock");
        
        // ✅ CHANGE: USER_NAME → ADMIN_NAME
        model.addAttribute("userName",
                (session != null && session.getAttribute("ADMIN_NAME") != null)
                        ? session.getAttribute("ADMIN_NAME") : "Admin");
        model.addAttribute("avatarUrl", 
                (session != null) ? session.getAttribute("ADMIN_AVATAR_URL") : null);

        // data
        model.addAttribute("hid", hid);
        model.addAttribute("rows",
                (hid == null) ? java.util.List.of() : donorRepository.getStockFromDonationsByHospital(hid));

        // dynamic hospital name
        model.addAttribute("hospitalName", resolveHospitalName(session, hid));

        return "admin/admin-bloodstock";
    }

    /** POST /admin/blood-stock/use */
    @PostMapping("/blood-stock/use")
    public String useBloodUnits(@RequestParam("bloodTypeId") int bloodTypeId,
                                @RequestParam(value = "hid", required = false) Integer hidParam,
                                @RequestParam("usedUnits") int usedUnits,
                                HttpSession session) {

        Integer hid = resolveHospitalId(session, hidParam);
        if (hid != null && usedUnits > 0) {
            donorRepository.useBloodUnits(bloodTypeId, hid, usedUnits);
        }
        return "redirect:/admin/bloodstock?hid=" + (hid == null ? "" : hid);
    }
}
