package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.Feedback;
import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import com.brandbuilder.reviewapp.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private ReviewRepository reviewRepository;

    @PostMapping("/review/{reviewId}")
    public ResponseEntity<?> createFeedback(
            @PathVariable Long reviewId,
            @RequestBody Feedback feedback) {

        System.out.println("=== Create Feedback Endpoint Called ===");
        System.out.println("Review ID: " + reviewId);
        System.out.println("Feedback: " + feedback.getFeedbackText());

        try {
            Feedback savedFeedback = feedbackService.createFeedback(feedback, reviewId);
            System.out.println("Feedback created successfully: " + savedFeedback.getId());

            // Return a simple success response to avoid circular reference
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Feedback submitted successfully",
                            "id", savedFeedback.getId()
                    ));
        } catch (Exception e) {
            System.err.println("Error creating feedback: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating feedback: " + e.getMessage());
        }
    }

    // NEW: Get feedback by review ID
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Feedback> getFeedbackByReviewId(@PathVariable Long reviewId) {
        System.out.println("=== Get Feedback by Review ID ===");
        System.out.println("Review ID: " + reviewId);

        try {
            Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
            if (reviewOpt.isEmpty()) {
                System.out.println("Review not found: " + reviewId);
                return ResponseEntity.notFound().build();
            }

            Review review = reviewOpt.get();
            Optional<Feedback> feedbackOpt = feedbackService.getFeedbackByReview(review);

            if (feedbackOpt.isPresent()) {
                System.out.println("Feedback found for review: " + reviewId);
                return ResponseEntity.ok(feedbackOpt.get());
            } else {
                System.out.println("No feedback found for review: " + reviewId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error getting feedback by review ID: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}