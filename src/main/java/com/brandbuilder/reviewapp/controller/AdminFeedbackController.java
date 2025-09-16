package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.Feedback;
import com.brandbuilder.reviewapp.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

// Admin Controller for Feedback Management
@RestController
@RequestMapping("/api/admin/feedback")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
class AdminFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllFeedback() {
        List<Feedback> feedbackList = feedbackService.getAllFeedback();
        List<Map<String, Object>> enrichedFeedback = feedbackList.stream()
                .map(this::enrichFeedbackWithReviewData)
                .toList();
        return ResponseEntity.ok(enrichedFeedback);
    }

    @GetMapping("/new")
    public ResponseEntity<List<Map<String, Object>>> getNewFeedback() {
        List<Feedback> feedbackList = feedbackService.getNewFeedback();
        List<Map<String, Object>> enrichedFeedback = feedbackList.stream()
                .map(this::enrichFeedbackWithReviewData)
                .toList();
        return ResponseEntity.ok(enrichedFeedback);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Map<String, Object>>> getFeedbackByStatus(@PathVariable String status) {
        try {
            Feedback.FeedbackStatus feedbackStatus = Feedback.FeedbackStatus.valueOf(status.toUpperCase());
            List<Feedback> feedbackList = feedbackService.getFeedbackByStatus(feedbackStatus);
            List<Map<String, Object>> enrichedFeedback = feedbackList.stream()
                    .map(this::enrichFeedbackWithReviewData)
                    .toList();
            return ResponseEntity.ok(enrichedFeedback);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/followup-required")
    public ResponseEntity<List<Map<String, Object>>> getFeedbackRequiringFollowup() {
        List<Feedback> feedbackList = feedbackService.getFeedbackRequiringFollowup();
        List<Map<String, Object>> enrichedFeedback = feedbackList.stream()
                .map(this::enrichFeedbackWithReviewData)
                .toList();
        return ResponseEntity.ok(enrichedFeedback);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getFeedbackById(@PathVariable Long id) {
        Optional<Feedback> feedback = feedbackService.getFeedbackById(id);
        return feedback.map(f -> ResponseEntity.ok(enrichFeedbackWithReviewData(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateFeedbackStatus(
            @PathVariable Long id,
            @RequestBody FeedbackStatusUpdateRequest request) {

        try {
            Feedback.FeedbackStatus status = Feedback.FeedbackStatus.valueOf(request.getStatus().toUpperCase());
            Feedback updatedFeedback = feedbackService.updateFeedbackStatus(id, status, request.getAdminResponse());
            return ResponseEntity.ok(enrichFeedbackWithReviewData(updatedFeedback));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status: " + request.getStatus());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error updating feedback status: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFeedback(@PathVariable Long id) {
        try {
            feedbackService.deleteFeedback(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error deleting feedback: " + e.getMessage());
        }
    }

    /**
     * NEW: Helper method to enrich feedback with review data for admin panel
     */
    private Map<String, Object> enrichFeedbackWithReviewData(Feedback feedback) {
        Map<String, Object> enrichedData = new HashMap<>();

        // Add all feedback fields
        enrichedData.put("id", feedback.getId());
        enrichedData.put("feedbackText", feedback.getFeedbackText());
        enrichedData.put("serviceQuality", feedback.getServiceQuality());
        enrichedData.put("staffBehavior", feedback.getStaffBehavior());
        enrichedData.put("cleanliness", feedback.getCleanliness());
        enrichedData.put("valueForMoney", feedback.getValueForMoney());
        enrichedData.put("overallExperience", feedback.getOverallExperience());
        enrichedData.put("suggestions", feedback.getSuggestions());
        enrichedData.put("contactEmail", feedback.getContactEmail());
        enrichedData.put("contactPhone", feedback.getContactPhone());
        enrichedData.put("wantsFollowup", feedback.getWantsFollowup());
        enrichedData.put("createdAt", feedback.getCreatedAt());
        enrichedData.put("status", feedback.getStatus());
        enrichedData.put("adminResponse", feedback.getAdminResponse());
        enrichedData.put("respondedAt", feedback.getRespondedAt());

        // Add review data if available
        if (feedback.getReview() != null) {
            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("id", feedback.getReview().getId());
            reviewData.put("rating", feedback.getReview().getRating());
            reviewData.put("comment", feedback.getReview().getComment());
            reviewData.put("createdAt", feedback.getReview().getCreatedAt());
            reviewData.put("isAnonymous", feedback.getReview().getIsAnonymous());

            // Customer information (handling anonymous reviews)
            if (feedback.getReview().getIsAnonymous()) {
                reviewData.put("customerName", "Anonymous Customer");
                reviewData.put("customerEmail", "");
            } else {
                // Check direct customer info first
                if (feedback.getReview().getCustomerName() != null && !feedback.getReview().getCustomerName().trim().isEmpty()) {
                    reviewData.put("customerName", feedback.getReview().getCustomerName());
                } else if (feedback.getReview().getCustomer() != null) {
                    reviewData.put("customerName", feedback.getReview().getCustomer().getName());
                } else {
                    reviewData.put("customerName", "Customer");
                }

                if (feedback.getReview().getCustomerEmail() != null && !feedback.getReview().getCustomerEmail().trim().isEmpty()) {
                    reviewData.put("customerEmail", feedback.getReview().getCustomerEmail());
                } else if (feedback.getReview().getCustomer() != null) {
                    reviewData.put("customerEmail", feedback.getReview().getCustomer().getEmail());
                } else {
                    reviewData.put("customerEmail", "");
                }
            }

            // Business information
            if (feedback.getReview().getBusinessProfile() != null) {
                Map<String, Object> businessData = new HashMap<>();
                businessData.put("id", feedback.getReview().getBusinessProfile().getId());
                businessData.put("businessName", feedback.getReview().getBusinessProfile().getBusinessName());
                businessData.put("address", feedback.getReview().getBusinessProfile().getAddress());
                reviewData.put("business", businessData);
            }

            enrichedData.put("review", reviewData);
        }

        return enrichedData;
    }

    // DTO for feedback status update
    public static class FeedbackStatusUpdateRequest {
        private String status;
        private String adminResponse;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getAdminResponse() {
            return adminResponse;
        }

        public void setAdminResponse(String adminResponse) {
            this.adminResponse = adminResponse;
        }
    }
}