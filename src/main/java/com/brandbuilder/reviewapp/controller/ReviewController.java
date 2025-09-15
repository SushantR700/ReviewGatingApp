package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.UserRepository;
import com.brandbuilder.reviewapp.service.CustomOAuth2User;
import com.brandbuilder.reviewapp.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    // Helper method to get user from authentication
    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            return oauth2User.getUser();
        } else if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauthToken.getPrincipal();

            String email = oauth2User.getAttribute("email");
            String providerId = oauth2User.getAttribute("sub");

            // Try to find user in database
            if (providerId != null) {
                Optional<User> userOpt = userRepository.findByProviderAndProviderId("GOOGLE", providerId);
                if (userOpt.isPresent()) {
                    return userOpt.get();
                }
            }

            // Fallback to email lookup
            if (email != null) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                return userOpt.orElse(null);
            }
        }

        return null;
    }

    @GetMapping("/business/{businessId}")
    public ResponseEntity<List<Review>> getReviewsByBusiness(@PathVariable Long businessId) {
        try {
            List<Review> reviews = reviewService.getReviewsByBusinessProfile(businessId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            System.err.println("Error fetching reviews for business " + businessId + ": " + e.getMessage());
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }

    @GetMapping("/my-reviews")
    public ResponseEntity<List<Review>> getMyReviews(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Review> reviews = reviewService.getReviewsByCustomer(user);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            System.err.println("Error fetching user reviews: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> getReviewById(@PathVariable Long id) {
        try {
            Optional<Review> review = reviewService.getReviewById(id);
            return review.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            System.err.println("Error fetching review " + id + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/check/{businessId}")
    public ResponseEntity<Boolean> hasReviewedBusiness(
            @PathVariable Long businessId,
            Authentication authentication) {

        System.out.println("=== hasReviewedBusiness called ===");
        System.out.println("Business ID: " + businessId);
        System.out.println("Authentication: " + authentication);
        System.out.println("Authentication type: " + (authentication != null ? authentication.getClass().getSimpleName() : "null"));

        User user = getUserFromAuthentication(authentication);
        System.out.println("Retrieved user: " + user);

        if (user == null) {
            System.out.println("No user found, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean hasReviewed = reviewService.hasCustomerReviewedBusiness(user, businessId);
            System.out.println("Has reviewed result: " + hasReviewed);
            return ResponseEntity.ok(hasReviewed);
        } catch (Exception e) {
            System.err.println("Error checking review status: " + e.getMessage());
            return ResponseEntity.ok(false); // Default to false if error
        }
    }

    @PostMapping("/business/{businessId}")
    public ResponseEntity<?> createReview(
            @PathVariable Long businessId,
            @RequestBody Review review,
            Authentication authentication) {

        System.out.println("=== Create Review Endpoint Called ===");
        System.out.println("Business ID: " + businessId);
        System.out.println("Rating: " + review.getRating());
        System.out.println("Comment: " + review.getComment());

        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            System.out.println("User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Please log in to leave a review");
        }

        System.out.println("User authenticated: " + user.getEmail());

        try {
            // Validate input
            if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Rating must be between 1 and 5");
            }

            Review savedReview = reviewService.createReview(review, user, businessId);
            System.out.println("Review created successfully: " + savedReview.getId());

            // Create response with proper logic
            ReviewResponse response = new ReviewResponse();
            response.setReview(savedReview);

            // FIXED LOGIC: High ratings (4-5) should redirect to Google
            // Low ratings (1-3) should show feedback form
            if (savedReview.getRating() >= 4) {
                response.setShouldRedirectToGoogle(true);
                response.setShouldShowFeedbackForm(false);
                System.out.println("High rating - will redirect to Google");
            } else if (savedReview.getRating() <= 3) {
                response.setShouldRedirectToGoogle(false);
                response.setShouldShowFeedbackForm(true);
                System.out.println("Low rating - will show feedback form");
            } else {
                // Default case (shouldn't happen with 1-5 rating)
                response.setShouldRedirectToGoogle(false);
                response.setShouldShowFeedbackForm(false);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.err.println("Error creating review: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating review: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long id,
            @RequestBody Review review,
            Authentication authentication) {

        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Please log in to update a review");
        }

        try {
            Review updatedReview = reviewService.updateReview(id, review, user);

            ReviewResponse response = new ReviewResponse();
            response.setReview(updatedReview);

            // Apply same logic for updated reviews
            if (updatedReview.getRating() >= 4) {
                response.setShouldRedirectToGoogle(true);
                response.setShouldShowFeedbackForm(false);
            } else if (updatedReview.getRating() <= 3) {
                response.setShouldRedirectToGoogle(false);
                response.setShouldShowFeedbackForm(true);
            } else {
                response.setShouldRedirectToGoogle(false);
                response.setShouldShowFeedbackForm(false);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error updating review: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error updating review: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Please log in to delete a review");
        }

        try {
            reviewService.deleteReview(id, user);
            return ResponseEntity.ok().body("Review deleted successfully");
        } catch (Exception e) {
            System.err.println("Error deleting review: " + e.getMessage());
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
        public Review getReview() {
            return review;
        }

        public void setReview(Review review) {
            this.review = review;
        }

        public boolean isShouldRedirectToGoogle() {
            return shouldRedirectToGoogle;
        }

        public void setShouldRedirectToGoogle(boolean shouldRedirectToGoogle) {
            this.shouldRedirectToGoogle = shouldRedirectToGoogle;
        }

        public boolean isShouldShowFeedbackForm() {
            return shouldShowFeedbackForm;
        }

        public void setShouldShowFeedbackForm(boolean shouldShowFeedbackForm) {
            this.shouldShowFeedbackForm = shouldShowFeedbackForm;
        }
    }
}