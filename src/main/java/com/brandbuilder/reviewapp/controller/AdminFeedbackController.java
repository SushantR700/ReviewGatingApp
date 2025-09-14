package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.Feedback;
import com.brandbuilder.reviewapp.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// Admin Controller for Feedback Management
@RestController
@RequestMapping("/api/admin/feedback")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
class AdminFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<List<Feedback>> getAllFeedback() {
        List<Feedback> feedback = feedbackService.getAllFeedback();
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/new")
    public ResponseEntity<List<Feedback>> getNewFeedback() {
        List<Feedback> feedback = feedbackService.getNewFeedback();
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Feedback>> getFeedbackByStatus(@PathVariable String status) {
        try {
            Feedback.FeedbackStatus feedbackStatus = Feedback.FeedbackStatus.valueOf(status.toUpperCase());
            List<Feedback> feedback = feedbackService.getFeedbackByStatus(feedbackStatus);
            return ResponseEntity.ok(feedback);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/followup-required")
    public ResponseEntity<List<Feedback>> getFeedbackRequiringFollowup() {
        List<Feedback> feedback = feedbackService.getFeedbackRequiringFollowup();
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Feedback> getFeedbackById(@PathVariable Long id) {
        Optional<Feedback> feedback = feedbackService.getFeedbackById(id);
        return feedback.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateFeedbackStatus(
            @PathVariable Long id,
            @RequestBody FeedbackStatusUpdateRequest request) {

        try {
            Feedback.FeedbackStatus status = Feedback.FeedbackStatus.valueOf(request.getStatus().toUpperCase());
            Feedback updatedFeedback = feedbackService.updateFeedbackStatus(id, status, request.getAdminResponse());
            return ResponseEntity.ok(updatedFeedback);
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
