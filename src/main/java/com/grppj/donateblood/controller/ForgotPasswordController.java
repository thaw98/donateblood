package com.grppj.donateblood.controller;

import com.grppj.donateblood.model.User;
import com.grppj.donateblood.repository.UserRepository;
import com.grppj.donateblood.service.EmailService;
import com.grppj.donateblood.util.OTPGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ForgotPasswordController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    // ✅ Store OTP + expiry time (5 minutes)
    private Map<String, OtpInfo> otpStorage = new HashMap<>();

    // Inner class to hold OTP and expiry
    private static class OtpInfo {
        String otp;
        LocalDateTime expiryTime;
        OtpInfo(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }

    // 1️⃣ Show forgot password page
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    // 2️⃣ Send OTP
    @PostMapping("/auth/forgot-password")
    public String sendOtp(@RequestParam("email") String email, Model model) {
        if (!userRepository.emailExists(email)) {
            model.addAttribute("msg", "Email not found.");
            return "forgot-password";
        }

        String otp = OTPGenerator.generateOTP(6);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(1);

        otpStorage.put(email, new OtpInfo(otp, expiryTime));
        emailService.sendOTP(email, otp);

        model.addAttribute("email", email);
        model.addAttribute("msg", "OTP sent to " + email + ". It will expire in 1 minute.");
        return "verify-otp";
    }

    // 3️⃣ Verify OTP
    @PostMapping("/auth/verify-otp")
    public String verifyOtp(@RequestParam("email") String email,
                            @RequestParam("otp") String otp,
                            Model model) {
        OtpInfo otpInfo = otpStorage.get(email);

        if (otpInfo == null) {
            model.addAttribute("email", email);
            model.addAttribute("msg", "No OTP found. Please resend OTP.");
            return "verify-otp";
        }

        if (LocalDateTime.now().isAfter(otpInfo.expiryTime)) {
            otpStorage.remove(email);
            model.addAttribute("email", email);
            model.addAttribute("msg", "OTP expired. Please resend a new OTP.");
            return "verify-otp";
        }

        if (otpInfo.otp.equals(otp)) {
            otpStorage.remove(email);
            model.addAttribute("email", email);
            return "reset-password";
        } else {
            model.addAttribute("email", email);
            model.addAttribute("msg", "Invalid OTP");
            return "verify-otp";
        }
    }

    // 4️⃣ Reset Password
    @PostMapping("/auth/reset-password")
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword,
                                Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("email", email);
            model.addAttribute("msg", "Passwords do not match");
            return "reset-password";
        }

        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            model.addAttribute("msg", "User not found");
            return "forgot-password";
        }

        userRepository.updateUserPassword(user.getId(), password);

        model.addAttribute("user", new User());
        model.addAttribute("msg", "Password reset successfully. Please log in with your new password.");
        return "login";
    }

    // 5️⃣ Resend OTP
    @PostMapping("/auth/resend-otp")
    @ResponseBody
    public String resendOtp(@RequestParam("email") String email) {
        if (!userRepository.emailExists(email)) {
            return "Email not found";
        }

        String otp = OTPGenerator.generateOTP(6);
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(1);
        otpStorage.put(email, new OtpInfo(otp, expiryTime));

        emailService.sendOTP(email, otp);

        return "OTP resent successfully";
    }
}
