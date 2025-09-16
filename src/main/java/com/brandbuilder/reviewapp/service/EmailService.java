package com.brandbuilder.reviewapp.service;

import com.brandbuilder.reviewapp.model.Feedback;
import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@reviewgate.com}")
    private String fromEmail;

    @Value("${app.name:ReviewGate}")
    private String appName;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * NEW: Send review notification to business owner when customer submits low rating
     * This is sent immediately when review is submitted (regardless of feedback form)
     * This is the ONLY email sent per review
     */
    public void sendReviewNotificationToBusiness(Review review) {
        try {
            BusinessProfile business = review.getBusinessProfile();
            User businessOwner = business.getCreatedBy();

            // Get customer information - handle both anonymous and non-anonymous reviews
            String customerName = getCustomerName(review);
            String customerEmail = getCustomerEmail(review);

            log.info("Sending review notification email to business owner: {} for business: {} from customer: {} (Rating: {}/5)",
                    businessOwner.getEmail(), business.getBusinessName(), customerName, review.getRating());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(businessOwner.getEmail());
            message.setSubject(String.format("New %d-Star Review for %s", review.getRating(), business.getBusinessName()));

            StringBuilder body = new StringBuilder();
            body.append("Hello ").append(businessOwner.getName()).append(",\n\n");
            body.append("You have received a new review for your business: ").append(business.getBusinessName()).append("\n\n");

            body.append("⭐ RATING: ").append(review.getRating()).append("/5 stars\n\n");

            if (review.getIsAnonymous()) {
                body.append("Customer: Anonymous Customer\n");
            } else {
                body.append("Customer: ").append(customerName);
                if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                    body.append(" (").append(customerEmail).append(")");
                }
                body.append("\n");
            }

            body.append("Date: ").append(review.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))).append("\n\n");

            if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
                body.append("REVIEW COMMENT:\n");
                body.append("\"").append(review.getComment()).append("\"\n\n");
            }

            // Add different messages based on rating
            if (review.getRating() <= 2) {
                body.append("This is a concerning low rating. ");
            } else if (review.getRating() == 3) {
                body.append("This is an average rating that could be improved. ");
            }

            body.append("The customer was shown a feedback form to provide more details about their experience. ");
            body.append("You can check if they provided additional feedback in your admin panel.\n\n");

            body.append("NEXT STEPS:\n");
            body.append("• Review this feedback carefully\n");
            body.append("• Identify areas for improvement\n");
            body.append("• Consider reaching out to address any concerns\n");
            if (!review.getIsAnonymous() && customerEmail != null && !customerEmail.trim().isEmpty()) {
                body.append("• Customer contact: ").append(customerEmail).append("\n");
            }
            body.append("\n");

            body.append("📊 View all reviews: ").append(frontendUrl).append("/admin\n");
            body.append("🏪 Your business page: ").append(frontendUrl).append("/").append(createBusinessSlug(business.getBusinessName())).append("\n\n");
            body.append("Best regards,\n").append(appName).append(" Team");

            message.setText(body.toString());
            emailSender.send(message);

            log.info("✅ Review notification email sent successfully to: {}", businessOwner.getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to send review notification email", e);
            // Don't throw exception to prevent review creation from failing
        }
    }

    /**
     * DEPRECATED: Keep for backward compatibility but don't use
     * We now send emails immediately when reviews are submitted, not when feedback is created
     */
    @Deprecated
    public void sendFeedbackNotificationToBusiness(Feedback feedback) {
        log.info("⚠️ sendFeedbackNotificationToBusiness called but emails are now sent when reviews are submitted, not when feedback is created");
        // Do nothing - emails are now sent when reviews are submitted
    }

    /**
     * DEPRECATED: Keep for backward compatibility but don't use
     */
    @Deprecated
    public void sendSimpleFeedbackNotification(Feedback feedback) {
        log.info("⚠️ sendSimpleFeedbackNotification called but emails are now sent when reviews are submitted, not when feedback is created");
        // Do nothing - emails are now sent when reviews are submitted
    }

    /**
     * Helper method to get customer name from review (handles both anonymous and non-anonymous)
     */
    private String getCustomerName(Review review) {
        if (review.getIsAnonymous()) {
            return "Anonymous Customer";
        }

        // Check direct customer info first (for anonymous reviews with name)
        if (review.getCustomerName() != null && !review.getCustomerName().trim().isEmpty()) {
            return review.getCustomerName();
        }

        // Check customer object (for logged-in users)
        if (review.getCustomer() != null) {
            if (review.getCustomer().getName() != null && !review.getCustomer().getName().trim().isEmpty()) {
                return review.getCustomer().getName();
            }
            if (review.getCustomer().getEmail() != null) {
                String emailPart = review.getCustomer().getEmail().split("@")[0];
                return emailPart.substring(0, 1).toUpperCase() + emailPart.substring(1);
            }
        }

        // Fallback
        return "Customer";
    }

    /**
     * Helper method to get customer email from review (handles both anonymous and non-anonymous)
     */
    private String getCustomerEmail(Review review) {
        if (review.getIsAnonymous()) {
            return ""; // Don't include email for anonymous reviews
        }

        // Check direct customer info first (for anonymous reviews with email)
        if (review.getCustomerEmail() != null && !review.getCustomerEmail().trim().isEmpty()) {
            return review.getCustomerEmail();
        }

        // Check customer object (for logged-in users)
        if (review.getCustomer() != null && review.getCustomer().getEmail() != null) {
            return review.getCustomer().getEmail();
        }

        return "";
    }

    /**
     * Helper method to create URL-friendly business name slug
     */
    private String createBusinessSlug(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            return "";
        }

        return businessName
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters except spaces and hyphens
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single hyphen
                .trim()
                .replaceAll("^-+|-+$", ""); // Remove leading/trailing hyphens
    }

    /**
     * Test email functionality
     */
    public void sendTestEmail(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Test Email from " + appName);
            message.setText("This is a test email to verify email configuration is working correctly.\n\nIf you receive this, email notifications are set up properly!");

            emailSender.send(message);
            log.info("Test email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            throw new RuntimeException("Email configuration error: " + e.getMessage());
        }
    }
}