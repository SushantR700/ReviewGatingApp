package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.UserRepository;
import com.brandbuilder.reviewapp.service.BusinessProfileService;
import com.brandbuilder.reviewapp.service.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    private boolean isAuthorizedUser(String email) {
        if (email == null) return false;

        // Check by email directly in database
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getRole() == User.Role.ADMIN;
        }

        // Fallback: allow specific emails
        return email.equals("sushantregmi419@gmail.com") ||
                email.endsWith("@brandbuilder.com");
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

    @GetMapping("/my-businesses")
    public ResponseEntity<?> getMyBusinessProfiles(Authentication authentication) {
        String email = getUserEmail(authentication);

        if (!isAuthorizedUser(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required. Current email: " + email);
        }

        try {
            User user = getUserFromEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not retrieve user information for email: " + email);
            }

            List<BusinessProfile> profiles = businessProfileService.getBusinessProfilesByAdmin(user);
            return ResponseEntity.ok(profiles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching businesses: " + e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBusinessProfile(
            @RequestPart BusinessProfile profile,
            @RequestPart(required = false) MultipartFile image,
            Authentication authentication) {

        String email = getUserEmail(authentication);

        if (!isAuthorizedUser(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
        }

        try {
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

        String email = getUserEmail(authentication);

        if (!isAuthorizedUser(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
        }

        try {
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
    public ResponseEntity<?> deleteBusinessProfile(@PathVariable Long id, Authentication authentication) {
        String email = getUserEmail(authentication);

        if (!isAuthorizedUser(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied. Admin role required.");
        }

        try {
            User user = getUserFromEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Could not retrieve user information");
            }

            businessProfileService.deleteBusinessProfile(id, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error deleting business profile: " + e.getMessage());
        }
    }
}