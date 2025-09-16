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
     * Send ONE complete email with review + feedback using your existing HTML template
     */
    public void sendCompleteReviewFeedbackNotification(Review review, Feedback feedback) {
        try {
            BusinessProfile business = review.getBusinessProfile();
            User businessOwner = business.getCreatedBy();

            // Get customer information
            String customerName = getCustomerName(review);
            String customerEmail = getCustomerEmail(review);

            log.info("Sending ONE complete email using HTML template to: {} for business: {} from customer: {} (Rating: {}/5)",
                    businessOwner.getEmail(), business.getBusinessName(), customerName, review.getRating());

            try {
                // Send HTML email using your existing feedback-notification.html template
                sendHtmlEmailWithTemplate(review, feedback, business, businessOwner, customerName, customerEmail);
                log.info("✅ HTML email sent successfully using feedback-notification.html template");
            } catch (Exception htmlError) {
                log.warn("Failed to send HTML email, falling back to plain text: {}", htmlError.getMessage());
                // Fallback to plain text
                sendPlainTextFallback(review, feedback, business, businessOwner, customerName, customerEmail);
                log.info("✅ Plain text email sent successfully as fallback");
            }

        } catch (Exception e) {
            log.error("❌ Failed to send complete email notification", e);
            // Don't throw exception to prevent feedback creation from failing
        }
    }

    /**
     * Send HTML email using your existing feedback-notification.html template
     */
    private void sendHtmlEmailWithTemplate(Review review, Feedback feedback, BusinessProfile business,
                                           User businessOwner, String customerName, String customerEmail) throws MessagingException {

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(businessOwner.getEmail());
        helper.setSubject(String.format("New customer feedback - %s", business.getBusinessName()));

        // Prepare template variables for your existing feedback-notification.html
        Context context = new Context();
        context.setVariable("appName", appName);
        context.setVariable("businessOwnerName", businessOwner.getName());
        context.setVariable("businessName", business.getBusinessName());

        // Review data
        context.setVariable("rating", review.getRating());
        context.setVariable("customerName", customerName);
        context.setVariable("customerEmail", customerEmail);
        context.setVariable("isAnonymous", review.getIsAnonymous());
        context.setVariable("submittedDate", review.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")));
        context.setVariable("reviewComment", review.getComment());

        // Feedback data
        context.setVariable("feedbackText", feedback.getFeedbackText());
        context.setVariable("serviceQuality", feedback.getServiceQuality());
        context.setVariable("staffBehavior", feedback.getStaffBehavior());
        context.setVariable("cleanliness", feedback.getCleanliness());
        context.setVariable("valueForMoney", feedback.getValueForMoney());
        context.setVariable("overallExperience", feedback.getOverallExperience());
        context.setVariable("suggestions", feedback.getSuggestions());
        context.setVariable("wantsFollowup", feedback.getWantsFollowup());
        context.setVariable("contactEmail", feedback.getContactEmail());
        context.setVariable("contactPhone", feedback.getContactPhone());

        // Process your existing template
        String htmlContent = templateEngine.process("feedback-notification", context);
        helper.setText(htmlContent, true);

        emailSender.send(message);
    }

    /**
     * Plain text fallback if HTML template fails
     */
    private void sendPlainTextFallback(Review review, Feedback feedback, BusinessProfile business,
                                       User businessOwner, String customerName, String customerEmail) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(businessOwner.getEmail());
        message.setSubject(String.format("New customer feedback - %s", business.getBusinessName()));

        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(businessOwner.getName()).append(",\n\n");
        body.append("You received new feedback for ").append(business.getBusinessName()).append(".\n\n");

        // Review section
        body.append("=== REVIEW DETAILS ===\n");
        body.append("Rating: ").append(review.getRating()).append("/5\n");
        body.append("Customer: ").append(customerName);
        if (!review.getIsAnonymous() && customerEmail != null && !customerEmail.trim().isEmpty()) {
            body.append(" (").append(customerEmail).append(")");
        }
        body.append("\n");
        body.append("Date: ").append(review.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))).append("\n");

        if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
            body.append("Review: \"").append(review.getComment()).append("\"\n");
        }
        body.append("\n");

        // Feedback section
        body.append("=== ADDITIONAL FEEDBACK ===\n");
        if (feedback.getFeedbackText() != null && !feedback.getFeedbackText().trim().isEmpty()) {
            body.append("Comments: \"").append(feedback.getFeedbackText()).append("\"\n");
        }

        if (feedback.getServiceQuality() != null && !feedback.getServiceQuality().trim().isEmpty()) {
            body.append("Service Quality: ").append(feedback.getServiceQuality()).append("\n");
        }
        if (feedback.getStaffBehavior() != null && !feedback.getStaffBehavior().trim().isEmpty()) {
            body.append("Staff Behavior: ").append(feedback.getStaffBehavior()).append("\n");
        }
        if (feedback.getCleanliness() != null && !feedback.getCleanliness().trim().isEmpty()) {
            body.append("Cleanliness: ").append(feedback.getCleanliness()).append("\n");
        }
        if (feedback.getValueForMoney() != null && !feedback.getValueForMoney().trim().isEmpty()) {
            body.append("Value for Money: ").append(feedback.getValueForMoney()).append("\n");
        }
        if (feedback.getOverallExperience() != null && !feedback.getOverallExperience().trim().isEmpty()) {
            body.append("Overall Experience: ").append(feedback.getOverallExperience()).append("\n");
        }

        if (feedback.getSuggestions() != null && !feedback.getSuggestions().trim().isEmpty()) {
            body.append("Suggestions: \"").append(feedback.getSuggestions()).append("\"\n");
        }

        if (feedback.getWantsFollowup() && !review.getIsAnonymous()) {
            body.append("\nCustomer requested follow-up:\n");
            if (feedback.getContactEmail() != null && !feedback.getContactEmail().trim().isEmpty()) {
                body.append("Email: ").append(feedback.getContactEmail()).append("\n");
            }
            if (feedback.getContactPhone() != null && !feedback.getContactPhone().trim().isEmpty()) {
                body.append("Phone: ").append(feedback.getContactPhone()).append("\n");
            }
        }

        body.append("\nBest regards,\n").append(appName).append(" Team");

        message.setText(body.toString());
        emailSender.send(message);
    }

    /**
     * Helper method to get customer name from review
     */
    private String getCustomerName(Review review) {
        if (review.getIsAnonymous()) {
            return "Anonymous Customer";
        }

        if (review.getCustomerName() != null && !review.getCustomerName().trim().isEmpty()) {
            return review.getCustomerName();
        }

        if (review.getCustomer() != null) {
            if (review.getCustomer().getName() != null && !review.getCustomer().getName().trim().isEmpty()) {
                return review.getCustomer().getName();
            }
            if (review.getCustomer().getEmail() != null) {
                String emailPart = review.getCustomer().getEmail().split("@")[0];
                return emailPart.substring(0, 1).toUpperCase() + emailPart.substring(1);
            }
        }

        return "Customer";
    }

    /**
     * Helper method to get customer email from review
     */
    private String getCustomerEmail(Review review) {
        if (review.getIsAnonymous()) {
            return "";
        }

        if (review.getCustomerEmail() != null && !review.getCustomerEmail().trim().isEmpty()) {
            return review.getCustomerEmail();
        }

        if (review.getCustomer() != null && review.getCustomer().getEmail() != null) {
            return review.getCustomer().getEmail();
        }

        return "";
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