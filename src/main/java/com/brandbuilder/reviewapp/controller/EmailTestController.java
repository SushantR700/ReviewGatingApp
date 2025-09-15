package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test/email")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send-test")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            String toEmail = request.get("email");
            if (toEmail == null || toEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email address is required");
            }

            emailService.sendTestEmail(toEmail);
            return ResponseEntity.ok().body("Test email sent successfully to " + toEmail);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send test email: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getEmailStatus() {
        try {
            // Try to send a test email to verify configuration
            return ResponseEntity.ok().body("Email service is configured and ready");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Email service not properly configured: " + e.getMessage());
        }
    }
}