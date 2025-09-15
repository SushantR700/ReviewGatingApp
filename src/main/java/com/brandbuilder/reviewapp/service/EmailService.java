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
     * Send feedback notification to business owner when customer submits feedback for low rating
     */
    public void sendFeedbackNotificationToBusiness(Feedback feedback) {
        try {
            Review review = feedback.getReview();
            BusinessProfile business = review.getBusinessProfile();
            User businessOwner = business.getCreatedBy();
            User customer = review.getCustomer();

            log.info("Sending feedback notification email to business owner: {} for business: {}",
                    businessOwner.getEmail(), business.getBusinessName());

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(businessOwner.getEmail());
            helper.setSubject(String.format("New Customer Feedback for %s", business.getBusinessName()));

            // Create context for template
            Context context = new Context();
            context.setVariable("businessOwnerName", businessOwner.getName());
            context.setVariable("businessName", business.getBusinessName());
            context.setVariable("customerName", customer.getName());
            context.setVariable("customerEmail", customer.getEmail());
            context.setVariable("rating", review.getRating());
            context.setVariable("reviewComment", review.getComment());
            context.setVariable("feedbackText", feedback.getFeedbackText());
            context.setVariable("serviceQuality", feedback.getServiceQuality());
            context.setVariable("staffBehavior", feedback.getStaffBehavior());
            context.setVariable("cleanliness", feedback.getCleanliness());
            context.setVariable("valueForMoney", feedback.getValueForMoney());
            context.setVariable("overallExperience", feedback.getOverallExperience());
            context.setVariable("suggestions", feedback.getSuggestions());
            context.setVariable("contactEmail", feedback.getContactEmail());
            context.setVariable("contactPhone", feedback.getContactPhone());
            context.setVariable("wantsFollowup", feedback.getWantsFollowup());
            context.setVariable("submittedDate", feedback.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")));
            context.setVariable("appName", appName);
            context.setVariable("businessUrl", frontendUrl + "/business/" + business.getId());
            context.setVariable("adminUrl", frontendUrl + "/admin");

            // Process HTML template
            String htmlContent = templateEngine.process("feedback-notification", context);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("Feedback notification email sent successfully to: {}", businessOwner.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send feedback notification email", e);
            // Don't throw exception to prevent feedback creation from failing
        } catch (Exception e) {
            log.error("Unexpected error sending feedback notification email", e);
        }
    }

    /**
     * Send simple feedback notification (fallback if HTML template fails)
     */
    public void sendSimpleFeedbackNotification(Feedback feedback) {
        try {
            Review review = feedback.getReview();
            BusinessProfile business = review.getBusinessProfile();
            User businessOwner = business.getCreatedBy();
            User customer = review.getCustomer();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(businessOwner.getEmail());
            message.setSubject(String.format("New Customer Feedback for %s", business.getBusinessName()));

            StringBuilder body = new StringBuilder();
            body.append("Hello ").append(businessOwner.getName()).append(",\n\n");
            body.append("You have received new feedback for your business: ").append(business.getBusinessName()).append("\n\n");
            body.append("Customer: ").append(customer.getName()).append(" (").append(customer.getEmail()).append(")\n");
            body.append("Rating: ").append(review.getRating()).append("/5 stars\n\n");

            if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
                body.append("Review Comment:\n").append(review.getComment()).append("\n\n");
            }

            if (feedback.getFeedbackText() != null && !feedback.getFeedbackText().trim().isEmpty()) {
                body.append("Additional Feedback:\n").append(feedback.getFeedbackText()).append("\n\n");
            }

            body.append("Feedback Details:\n");
            if (feedback.getServiceQuality() != null) {
                body.append("Service Quality: ").append(feedback.getServiceQuality()).append("\n");
            }
            if (feedback.getStaffBehavior() != null) {
                body.append("Staff Behavior: ").append(feedback.getStaffBehavior()).append("\n");
            }
            if (feedback.getCleanliness() != null) {
                body.append("Cleanliness: ").append(feedback.getCleanliness()).append("\n");
            }
            if (feedback.getValueForMoney() != null) {
                body.append("Value for Money: ").append(feedback.getValueForMoney()).append("\n");
            }
            if (feedback.getOverallExperience() != null) {
                body.append("Overall Experience: ").append(feedback.getOverallExperience()).append("\n");
            }

            if (feedback.getSuggestions() != null && !feedback.getSuggestions().trim().isEmpty()) {
                body.append("\nSuggestions for Improvement:\n").append(feedback.getSuggestions()).append("\n");
            }

            if (feedback.getWantsFollowup() && feedback.getContactEmail() != null) {
                body.append("\nCustomer wants follow-up. Contact: ").append(feedback.getContactEmail());
                if (feedback.getContactPhone() != null) {
                    body.append(" / ").append(feedback.getContactPhone());
                }
                body.append("\n");
            }

            body.append("\nView your business dashboard: ").append(frontendUrl).append("/admin\n\n");
            body.append("Best regards,\n").append(appName).append(" Team");

            message.setText(body.toString());
            emailSender.send(message);

            log.info("Simple feedback notification email sent successfully to: {}", businessOwner.getEmail());

        } catch (Exception e) {
            log.error("Failed to send simple feedback notification email", e);
        }
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