package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.UserRepository;
import com.brandbuilder.reviewapp.repo.FeedbackRepository;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import com.brandbuilder.reviewapp.service.BusinessProfileService;
import com.brandbuilder.reviewapp.service.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/businesses")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AdminBusinessProfileController {

    @Autowired
    private BusinessProfileService businessProfileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @GetMapping("/my-businesses")
    public ResponseEntity<?> getMyBusinessProfiles(Authentication authentication) {
        System.out.println("=== getMyBusinessProfiles called ===");

        try {
            String email = getUserEmail(authentication);
            User user = getUserFromEmail(email);
            if (user == null) {
                System.out.println("User not found for email: " + email);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not retrieve user information for email: " + email);
            }

            List<BusinessProfile> profiles = businessProfileService.getBusinessProfilesByAdmin(user);
            System.out.println("Found " + profiles.size() + " businesses for user");

            return ResponseEntity.ok(profiles);
        } catch (Exception e) {
            System.err.println("Error fetching businesses: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching businesses: " + e.getMessage());
        }
    }

    private String getUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            return oauth2User.getEmail();
        } else if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauthToken.getPrincipal();
            return oauth2User.getAttribute("email");
        }

        return null;
    }

    private User getUserFromEmail(String email) {
        if (email == null) return null;
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.orElse(null);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBusinessProfile(
            @RequestPart BusinessProfile profile,
            @RequestPart(required = false) MultipartFile image,
            Authentication authentication) {

        try {
            String email = getUserEmail(authentication);
            User user = getUserFromEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not retrieve user information");
            }

            BusinessProfile savedProfile = businessProfileService.createBusinessProfile(profile, image, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProfile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing image: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error creating business profile: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBusinessProfile(
            @PathVariable Long id,
            @RequestPart BusinessProfile profile,
            @RequestPart(required = false) MultipartFile image,
            Authentication authentication) {

        try {
            String email = getUserEmail(authentication);
            User user = getUserFromEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not retrieve user information");
            }

            BusinessProfile updatedProfile = businessProfileService.updateBusinessProfile(id, profile, image, user);
            return ResponseEntity.ok(updatedProfile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing image: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error updating business profile: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteBusinessProfile(@PathVariable Long id, Authentication authentication) {
        try {
            String email = getUserEmail(authentication);
            User user = getUserFromEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not retrieve user information");
            }

            Optional<BusinessProfile> profileOpt = businessProfileService.getBusinessProfileById(id);
            if (profileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BusinessProfile profile = profileOpt.get();

            // Check ownership
            if (!profile.getCreatedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Unauthorized to delete this business profile");
            }

            // Manual cascade deletion to avoid foreign key issues
            System.out.println("=== Starting manual cascade deletion ===");

            // 1. Delete all feedback for reviews of this business
            List<com.brandbuilder.reviewapp.model.Review> reviews = reviewRepository.findByBusinessProfile(profile);
            for (com.brandbuilder.reviewapp.model.Review review : reviews) {
                feedbackRepository.findByReview(review).ifPresent(feedback -> {
                    System.out.println("Deleting feedback ID: " + feedback.getId());
                    feedbackRepository.delete(feedback);
                });
            }

            // 2. Delete all reviews for this business
            for (com.brandbuilder.reviewapp.model.Review review : reviews) {
                System.out.println("Deleting review ID: " + review.getId());
                reviewRepository.delete(review);
            }

            // 3. Delete the business profile
            System.out.println("Deleting business profile ID: " + profile.getId());
            businessProfileService.deleteBusinessProfile(id, user);

            System.out.println("=== Cascade deletion completed successfully ===");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("Error deleting business profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error deleting business profile: " + e.getMessage());
        }
    }
}