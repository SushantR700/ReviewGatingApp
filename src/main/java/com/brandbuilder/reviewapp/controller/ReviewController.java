package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.service.CustomOAuth2User;
import com.brandbuilder.reviewapp.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/business/{businessId}")
    public ResponseEntity<List<Review>> getReviewsByBusiness(@PathVariable Long businessId) {
        List<Review> reviews = reviewService.getReviewsByBusinessProfile(businessId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<List<Review>> getMyReviews(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        List<Review> reviews = reviewService.getReviewsByCustomer(oauth2User.getUser());
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> getReviewById(@PathVariable Long id) {
        Optional<Review> review = reviewService.getReviewById(id);
        return review.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/check/{businessId}")
    public ResponseEntity<Boolean> hasReviewedBusiness(
            @PathVariable Long businessId,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        boolean hasReviewed = reviewService.hasCustomerReviewedBusiness(oauth2User.getUser(), businessId);
        return ResponseEntity.ok(hasReviewed);
    }

    @PostMapping("/business/{businessId}")
    public ResponseEntity<?> createReview(
            @PathVariable Long businessId,
            @RequestBody Review review,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            Review savedReview = reviewService.createReview(review, oauth2User.getUser(), businessId);

            // Return response with redirect information
            ReviewResponse response = new ReviewResponse();
            response.setReview(savedReview);
            response.setShouldRedirectToGoogle(savedReview.getRedirectedToGoogle());
            response.setShouldShowFeedbackForm(savedReview.getRating() <= 3);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating review: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long id,
            @RequestBody Review review,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            Review updatedReview = reviewService.updateReview(id, review, oauth2User.getUser());

            ReviewResponse response = new ReviewResponse();
            response.setReview(updatedReview);
            response.setShouldRedirectToGoogle(updatedReview.getRedirectedToGoogle());
            response.setShouldShowFeedbackForm(updatedReview.getRating() <= 3);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error updating review: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            reviewService.deleteReview(id, oauth2User.getUser());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error deleting review: " + e.getMessage());
        }
    }

    // DTO for review response
    public static class ReviewResponse {
        private Review review;
        private boolean shouldRedirectToGoogle;
        private boolean shouldShowFeedbackForm;

        // Getters and setters
        public Review getReview() { return review; }
        public void setReview(Review review) { this.review = review; }

        public boolean isShouldRedirectToGoogle() { return shouldRedirectToGoogle; }
        public void setShouldRedirectToGoogle(boolean shouldRedirectToGoogle) { this.shouldRedirectToGoogle = shouldRedirectToGoogle; }

        public boolean isShouldShowFeedbackForm() { return shouldShowFeedbackForm; }
        public void setShouldShowFeedbackForm(boolean shouldShowFeedbackForm) { this.shouldShowFeedbackForm = shouldShowFeedbackForm; }
    }
}

