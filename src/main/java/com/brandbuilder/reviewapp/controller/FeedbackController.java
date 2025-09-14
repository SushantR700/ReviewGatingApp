package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.Feedback;
import com.brandbuilder.reviewapp.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/review/{reviewId}")
    public ResponseEntity<?> createFeedback(
            @PathVariable Long reviewId,
            @RequestBody Feedback feedback) {

        try {
            Feedback savedFeedback = feedbackService.createFeedback(feedback, reviewId);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFeedback);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating feedback: " + e.getMessage());
        }
    }

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Feedback> getFeedbackByReviewId(@PathVariable Long reviewId) {
        // This would require a service method to find review first, then get feedback
        // For now, returning not implemented
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}

