package com.grppj.donateblood.controller;

import com.grppj.donateblood.model.User;
import com.grppj.donateblood.repository.BloodRequestRepository;
import com.grppj.donateblood.repository.DashboardRepository;
import com.grppj.donateblood.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DashboardRepository dashboardRepo;
    private final UserRepository userRepo;
    private final BloodRequestRepository bloodRequestRepo;

    public AdminController(DashboardRepository dashboardRepo,
                           UserRepository userRepo,
                           BloodRequestRepository bloodRequestRepo) {
        this.dashboardRepo = dashboardRepo;
        this.userRepo = userRepo;
        this.bloodRequestRepo = bloodRequestRepo;
    }

    /* ===================== DASHBOARD ===================== */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@RequestParam(name = "range", required = false, defaultValue = "30d") String range,
                            Model model, HttpSession session) {

        if (session.getAttribute("loginuser") == null) return "redirect:/login";

        // topnav bits
        model.addAttribute("active", "admin");
        model.addAttribute("userName", session.getAttribute("ADMIN_NAME") != null
                ? (String) session.getAttribute("ADMIN_NAME") : "Admin");
        model.addAttribute("avatarUrl", session.getAttribute("ADMIN_AVATAR_URL"));

        Integer hospitalId = (Integer) session.getAttribute("HOSPITAL_ID");

        // KPIs
        if (hospitalId != null) {
            model.addAttribute("totalBloodDonated", dashboardRepo.totalBloodDonatedUnitsByHospital(hospitalId));
            model.addAttribute("pendingDonors", dashboardRepo.pendingDonorsByHospital(hospitalId));
            model.addAttribute("totalAppointments", dashboardRepo.totalAppointmentsByHospital(hospitalId));
            model.addAttribute("pendingBloodRequests", dashboardRepo.pendingBloodRequestsByHospital(hospitalId));
        } else {
            model.addAttribute("totalBloodDonated", dashboardRepo.totalBloodDonatedUnits());
            model.addAttribute("pendingDonors", dashboardRepo.pendingDonors());
            model.addAttribute("totalAppointments", dashboardRepo.totalAppointments());
            model.addAttribute("pendingBloodRequests", dashboardRepo.pendingBloodRequests());
        }

        // Chart: donations overview (hospital only)
        model.addAttribute("range", range);
        List<String> btOrder = Arrays.asList("A+","A-","B+","B-","O+","O-","AB+","AB-");
        Map<String, List<Integer>> seriesMap = new LinkedHashMap<>();
        for (String bt : btOrder) seriesMap.put(bt, new ArrayList<>());
        List<String> labels;
        String subtitle;

        if (hospitalId == null) {
            labels = Collections.emptyList();
            subtitle = "Select a hospital to view donations";
        } else {
            LocalDate today = LocalDate.now();
            DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            if ("7d".equalsIgnoreCase(range) || "30d".equalsIgnoreCase(range)) {
                int days = "7d".equalsIgnoreCase(range) ? 7 : 30;
                LocalDate start = today.minusDays(days - 1);
                LocalDate end = today;

                DateTimeFormatter labFmt = DateTimeFormatter.ofPattern("MMM d");
                labels = new ArrayList<>(days);
                List<String> buckets = new ArrayList<>(days);
                for (int i = 0; i < days; i++) {
                    LocalDate d = start.plusDays(i);
                    labels.add(labFmt.format(d));
                    buckets.add(d.format(iso));
                }

                List<Map<String, Object>> rows =
                        dashboardRepo.donationsDailyByBloodTypeForHospital(
                                start.format(iso), end.format(iso), hospitalId);

                Map<String, Integer> tmp = new HashMap<>();
                for (Map<String,Object> r : rows) {
                    String b = String.valueOf(r.get("bucket"));
                    String bt = String.valueOf(r.get("bt"));
                    int u = ((Number) r.get("units")).intValue();
                    tmp.put(b + "|" + bt, u);
                }

                for (String bt : btOrder) {
                    List<Integer> line = seriesMap.get(bt);
                    for (String bk : buckets) line.add(tmp.getOrDefault(bk + "|" + bt, 0));
                }
                subtitle = "Daily (" + days + " days)";
            } else { // 3m
                YearMonth now = YearMonth.now();
                YearMonth startYm = now.minusMonths(2);
                LocalDate start = startYm.atDay(1);
                LocalDate end   = now.atEndOfMonth();

                List<String> buckets = new ArrayList<>(3);
                labels = new ArrayList<>(3);
                for (int i = 0; i < 3; i++) {
                    YearMonth ym = startYm.plusMonths(i);
                    buckets.add(ym.toString()); // YYYY-MM
                    labels.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                }

                List<Map<String,Object>> rows =
                        dashboardRepo.donationsMonthlyByBloodTypeForHospital(
                                start.toString(), end.toString(), hospitalId);

                Map<String, Integer> tmp = new HashMap<>();
                for (Map<String,Object> r : rows) {
                    String b = String.valueOf(r.get("bucket"));
                    String bt = String.valueOf(r.get("bt"));
                    int u = ((Number) r.get("units")).intValue();
                    tmp.put(b + "|" + bt, u);
                }

                for (String bt : btOrder) {
                    List<Integer> line = seriesMap.get(bt);
                    for (String bk : buckets) line.add(tmp.getOrDefault(bk + "|" + bt, 0));
                }
                subtitle = "Monthly (last 3 months)";
            }
        }

        model.addAttribute("donationLabels", labels);
        model.addAttribute("donationBtOrder", btOrder);
        model.addAttribute("donationSeries", seriesMap);
        model.addAttribute("donationSubtitle", subtitle);

        // Recent appointments (limit 4)
        model.addAttribute("recentAppointments",
                (hospitalId != null) ? dashboardRepo.recentAppointmentsByHospital(4, hospitalId) : List.of());
        model.addAttribute("recentApptNote",
                hospitalId == null ? "Select a hospital to see its recent appointments." : null);

        // ===== NEW: Recent blood requests (limit 4) =====
        if (hospitalId != null) {
            model.addAttribute("recentRequests", bloodRequestRepo.recentRequestsByHospital(4, hospitalId));
            model.addAttribute("recentReqNote", null);
        } else {
            model.addAttribute("recentRequests", bloodRequestRepo.recentRequests(4));
            model.addAttribute("recentReqNote", "Showing latest requests across all hospitals.");
        }

        return "admin/dashboard";
    }

    /* ===================== PROFILE ===================== */

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        if (session.getAttribute("loginuser") == null) return "redirect:/login";

        // figure out current admin from session
        User admin = resolveCurrentAdmin(session);
        if (admin == null) return "redirect:/login";

        // reload from DB to ensure freshness (so cards show latest name)
        User fresh = userRepo.getUserById(admin.getId());

        // keep session display name in sync (top-nav already uses this)
        session.setAttribute("ADMIN_NAME", fresh.getUsername());
        session.setAttribute("loginuser", fresh); // refresh session user object

        // topnav & sidebar
        model.addAttribute("active", "admin");
        model.addAttribute("userName", fresh.getUsername());
        model.addAttribute("avatarUrl", session.getAttribute("ADMIN_AVATAR_URL"));

        // profile fields
        model.addAttribute("profileName", fresh.getUsername());
        model.addAttribute("profileEmail", fresh.getEmail());
        model.addAttribute("profileHospital",
                session.getAttribute("HOSPITAL_NAME") != null ? session.getAttribute("HOSPITAL_NAME") : "—");

        return "admin/profile";
    }

    @PostMapping("/profile")
    public String profileSave(@RequestParam("name") String name,
                              @RequestParam(value = "newPassword", required = false) String newPassword,
                              @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                              HttpSession session,
                              RedirectAttributes ra) {
        if (session.getAttribute("loginuser") == null) return "redirect:/login";

        User admin = resolveCurrentAdmin(session);
        if (admin == null) return "redirect:/login";

        // update name
        if (name != null && !name.isBlank()) {
            userRepo.updateUserName(admin.getId(), name.trim());
        }

        // update password if provided
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 6) {
                ra.addFlashAttribute("error", "Password must be at least 6 characters.");
                return "redirect:/admin/profile";
            }
            if (!Objects.equals(newPassword, confirmPassword)) {
                ra.addFlashAttribute("error", "New password and confirmation do not match.");
                return "redirect:/admin/profile";
            }
            userRepo.updateUserPassword(admin.getId(), newPassword);
        }

        // refresh & sync session so UI shows latest everywhere
        User fresh = userRepo.getUserById(admin.getId());
        session.setAttribute("loginuser", fresh);
        session.setAttribute("ADMIN_NAME", fresh.getUsername());

        ra.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/admin/profile";
    }

    private User resolveCurrentAdmin(HttpSession session) {
        Object obj = session.getAttribute("loginuser");
        if (obj instanceof User u) return u;

        // fallback by email if your app stores ADMIN_EMAIL
        Object emailObj = session.getAttribute("ADMIN_EMAIL");
        if (emailObj instanceof String em && !em.isBlank()) {
            try {
                return userRepo.getUserByEmail(em);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
