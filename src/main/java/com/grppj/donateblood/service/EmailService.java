package com.grppj.donateblood.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Plain text OTP method (keep for backward compatibility)
    public void sendOTP(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP code is: " + otp + "\n\nThis code is valid for 1 minute.");
        mailSender.send(message);
    }

    // ✅ UPDATED: HTML Email for Verification Code
    public void sendVerificationCode(String toEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Your Verification Code - Blood Donation System");
            
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #dc3545; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .code { background: #f8f9fa; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; margin: 20px 0; border: 2px dashed #dc3545; border-radius: 5px; }
                        .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🏥 Blood Donation System</h1>
                            <h2>Admin Verification Code</h2>
                        </div>
                        
                        <p>Hello,</p>
                        <p>Please use the following verification code to access your admin account:</p>
                        
                        <div class="code">
                            %s
                        </div>
                        
                        <p><strong>This code will expire in 10 minutes.</strong></p>
                        <p>If you didn't request this code, please ignore this email.</p>
                        
                        <div class="footer">
                            <p>This is an automated message. Please do not reply to this email.</p>
                            <p>&copy; 2024 Blood Donation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(verificationCode);
            
            helper.setText(htmlContent, true); // true = HTML content
            mailSender.send(message);
            System.out.println("Verification code email sent to: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("Failed to send verification email to: " + toEmail);
            e.printStackTrace();
            // Fallback to plain text
            sendOTP(toEmail, "Your verification code: " + verificationCode);
        }
    }

    // ✅ UPDATED: HTML Email for Assignment Notification
    public void sendAssignmentNotification(String toEmail, String hospitalName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Hospital Assignment - Blood Donation System");
            
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #28a745; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .assignment { background: #f8f9fa; padding: 20px; margin: 20px 0; border-left: 4px solid #28a745; border-radius: 5px; }
                        .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🏥 Hospital Assignment</h1>
                        </div>
                        
                        <p>Dear Admin,</p>
                        
                        <div class="assignment">
                            <h3>📋 Assignment Notification</h3>
                            <p><strong>You have been assigned to manage:</strong></p>
                            <h2 style="color: #28a745; text-align: center;">%s</h2>
                        </div>
                        
                        <p>You can now access your admin dashboard to manage blood donation activities for this hospital.</p>
                        
                        <p style="text-align: center;">
                            <a href="http://localhost:8080/admin/dashboard" style="background: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">Go to Dashboard</a>
                        </p>
                        
                        <div class="footer">
                            <p>This is an automated message. Please do not reply to this email.</p>
                            <p>&copy; 2024 Blood Donation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(hospitalName);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("Assignment notification sent to: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("Failed to send assignment notification to: " + toEmail);
            e.printStackTrace();
            // Fallback to plain text
            sendOTP(toEmail, "You have been assigned to hospital: " + hospitalName);
        }
    }

    // ✅ UPDATED: HTML Email for Admin Welcome
    public void sendAdminWelcomeEmail(String toEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Blood Donation System - Admin Account Created");
            
            String username = toEmail.split("@")[0];
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #dc3545; color: white; padding: 20px; text-align: center; border-radius: 5px; }
                        .details { background: #f8f9fa; padding: 20px; margin: 20px 0; border-radius: 5px; }
                        .code { background: white; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; margin: 15px 0; border: 2px dashed #dc3545; border-radius: 5px; }
                        .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🏥 Blood Donation System</h1>
                            <h2>Welcome Admin!</h2>
                        </div>
                        
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Your administrator account has been successfully created in the Blood Donation System.</p>
                        
                        <div class="details">
                            <h3>📋 Your Login Details:</h3>
                            <p><strong>Username:</strong> %s</p>
                            <p><strong>Email:</strong> %s</p>
                            <p><strong>Verification Code:</strong></p>
                            <div class="code">%s</div>
                        </div>
                        
                        <h3>🚀 How to Access Your Account:</h3>
                        <ol>
                            <li>Go to: <a href="http://localhost:8080/admin/login">Admin Login Page</a></li>
                            <li>Enter your email address</li>
                            <li>Check your email for a verification code</li>
                            <li>Enter the code above to access your dashboard</li>
                        </ol>
                        
                        <p style="text-align: center;">
                            <a href="http://localhost:8080/admin/login" style="background: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block;">Login to Dashboard</a>
                        </p>
                        
                        <div class="footer">
                            <p>This is an automated message. Please do not reply to this email.</p>
                            <p>&copy; 2024 Blood Donation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, username, toEmail, verificationCode);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("Admin welcome email sent to: " + toEmail);
            
        } catch (Exception e) {
            System.err.println("Failed to send admin welcome email to: " + toEmail);
            e.printStackTrace();
            // Fallback to plain text
            sendOTP(toEmail, "Welcome! Your admin account has been created. Verification code: " + verificationCode);
        }
    }
}