package com.grppj.donateblood.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    // Plain text OTP method (keep for backward compatibility)
    public void sendOTP(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP code is: " + otp + "\n\n This code is valid for 1 minute.");
        mailSender.send(message);
    }
    
    public void sendPassword(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your Account Password");
        message.setText("Hello,\n\n" +
                        "Your account has been created successfully.\n" +
                        "Please use the following one-time password (OTP) as your login password:\n\n" +
                        otp + "\n\n" +
                        "You can use this password to log in and access your donor profile.\n\n" +
                        "After logging in, we recommend updating your password for security.\n\n" +
                        "Thank you for joining our blood donation community!\n" +
                        "— The Blood Donation Team");
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
                        
                         <p style="text-align: center;">
            		        <a href="http://localhost:8080/admin/login" class="btn">Go to Admin Login</a>
            		    </p>
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
            sendOTP(toEmail, "Your verification code: " + verificationCode + "\nLogin here: http://localhost:8080/admin/login");
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
 
}