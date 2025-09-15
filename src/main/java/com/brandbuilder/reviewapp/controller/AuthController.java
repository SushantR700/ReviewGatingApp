package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.UserRepository;
import com.brandbuilder.reviewapp.service.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        System.out.println("=== getCurrentUser called ===");
        System.out.println("Authentication: " + authentication);

        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("No authentication found");
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }

        System.out.println("Is authenticated: " + authentication.isAuthenticated());

        // Handle both CustomOAuth2User and regular OAuth2User
        if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            System.out.println("CustomOAuth2User found");

            response.put("authenticated", true);
            response.put("id", oauth2User.getUserId());
            response.put("email", oauth2User.getEmail());
            response.put("name", oauth2User.getName());
            response.put("role", oauth2User.getRole().name());
        } else if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauthToken.getPrincipal();

            System.out.println("OAuth2AuthenticationToken found:");
            System.out.println("- Email: " + oauth2User.getAttribute("email"));
            System.out.println("- Provider ID: " + oauth2User.getAttribute("sub"));

            String email = oauth2User.getAttribute("email");
            String providerId = oauth2User.getAttribute("sub");

            // Find user in database
            Optional<User> userOpt = userRepository.findByProviderAndProviderId("GOOGLE", providerId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("User found in database:");
                System.out.println("- User ID: " + user.getId());
                System.out.println("- Email: " + user.getEmail());
                System.out.println("- Role: " + user.getRole());

                response.put("authenticated", true);
                response.put("id", user.getId());
                response.put("email", user.getEmail());
                response.put("name", user.getName());
                response.put("role", user.getRole().name());
            } else {
                System.out.println("User not found in database, returning OAuth2 data with default role");
                response.put("authenticated", true);
                response.put("email", email);
                response.put("name", oauth2User.getAttribute("name"));
                response.put("role", "CUSTOMER"); // Default role
            }
        } else {
            System.out.println("Unknown authentication type: " + authentication.getPrincipal().getClass());
            response.put("authenticated", false);
        }

        System.out.println("Final response: " + response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(Authentication authentication) {
        System.out.println("=== getAuthStatus called ===");
        System.out.println("Authentication: " + authentication);

        Map<String, Object> status = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()) {
            status.put("authenticated", true);

            if (authentication.getPrincipal() instanceof CustomOAuth2User) {
                CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
                status.put("role", oauth2User.getRole().name());
            } else if (authentication instanceof OAuth2AuthenticationToken) {
                // Try to get user from database
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                OAuth2User oauth2User = oauthToken.getPrincipal();
                String providerId = oauth2User.getAttribute("sub");

                Optional<User> userOpt = userRepository.findByProviderAndProviderId("GOOGLE", providerId);
                if (userOpt.isPresent()) {
                    status.put("role", userOpt.get().getRole().name());
                } else {
                    status.put("role", "CUSTOMER");
                }
            } else {
                status.put("role", "CUSTOMER");
            }
        } else {
            status.put("authenticated", false);
            status.put("role", null);
        }

        System.out.println("Status: " + status);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/upgrade-to-admin")
    public ResponseEntity<?> upgradeToAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get current user email
            String email = null;
            String providerId = null;

            if (authentication.getPrincipal() instanceof CustomOAuth2User) {
                CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
                email = oauth2User.getEmail();
                providerId = oauth2User.getUser().getProviderId();
            } else if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                OAuth2User oauth2User = oauthToken.getPrincipal();
                email = oauth2User.getAttribute("email");
                providerId = oauth2User.getAttribute("sub");
            }

            if (email == null || providerId == null) {
                return ResponseEntity.badRequest().body("Could not determine user information");
            }

            // Only allow specific emails to upgrade (security measure)
            if (!email.equals("sushantregmi419@gmail.com") &&
                    !email.equals("junkiethunder@gmail.com") &&
                    !email.endsWith("@brandbuilder.com")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to upgrade to admin");
            }

            // Find and update user
            Optional<User> userOpt = userRepository.findByProviderAndProviderId("GOOGLE", providerId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setRole(User.Role.ADMIN);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);

                return ResponseEntity.ok().body("Successfully upgraded to admin. Please refresh the page to see changes.");
            } else {
                return ResponseEntity.badRequest().body("User not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error upgrading to admin: " + e.getMessage());
        }
    }
}