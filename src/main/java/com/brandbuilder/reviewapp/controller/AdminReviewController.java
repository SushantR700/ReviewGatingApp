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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

// Admin Controller for Reviews
@RestController
@RequestMapping("/api/admin/reviews")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
class AdminReviewController {

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
            String providerId = oauth2User.getAttribute("sub");

            if (providerId != null) {
                Optional<User> userOpt = userRepository.findByProviderAndProviderId("GOOGLE", providerId);
                return userOpt.orElse(null);
            }
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {
        List<Review> reviews = reviewService.getAllReviews();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/low-rating")
    public ResponseEntity<List<Review>> getLowRatingReviews() {
        List<Review> reviews = reviewService.getLowRatingReviews();
        return ResponseEntity.ok(reviews);
    }

    // NEW: Get reviews for a specific business (business owner access)
    @GetMapping("/business/{businessId}")
    public ResponseEntity<?> getReviewsForBusiness(@PathVariable Long businessId, Authentication authentication) {
        System.out.println("=== Admin getting reviews for business: " + businessId + " ===");

        try {
            User user = getUserFromAuthentication(authentication);
            if (user == null) {
                System.out.println("No authenticated user found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
            }

            System.out.println("User: " + user.getName() + " (" + user.getRole() + ")");

            // Check if user is admin/business owner
            if (user.getRole() != User.Role.ADMIN) {
                System.out.println("User is not an admin");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
            }

            // Get reviews for the business with ownership check
            List<Review> reviews = reviewService.getReviewsForBusinessOwner(businessId, user);
            System.out.println("Found " + reviews.size() + " reviews for business " + businessId);

            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            System.err.println("Error getting reviews for business: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error retrieving reviews: " + e.getMessage());
        }
    }
}