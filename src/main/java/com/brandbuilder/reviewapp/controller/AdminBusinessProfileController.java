package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.service.BusinessProfileService;
import com.brandbuilder.reviewapp.service.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// Admin Controller for Business Profiles
@RestController
@RequestMapping("/api/admin/businesses")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
class AdminBusinessProfileController {

    @Autowired
    private BusinessProfileService businessProfileService;

    @GetMapping("/my-businesses")
    public ResponseEntity<List<BusinessProfile>> getMyBusinessProfiles(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        List<BusinessProfile> profiles = businessProfileService.getBusinessProfilesByAdmin(oauth2User.getUser());
        return ResponseEntity.ok(profiles);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBusinessProfile(
            @RequestPart BusinessProfile profile,
            @RequestPart(required = false) MultipartFile image,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            BusinessProfile savedProfile = businessProfileService.createBusinessProfile(profile, image, oauth2User.getUser());
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

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            BusinessProfile updatedProfile = businessProfileService.updateBusinessProfile(id, profile, image, oauth2User.getUser());
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
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            businessProfileService.deleteBusinessProfile(id, oauth2User.getUser());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error deleting business profile: " + e.getMessage());
        }
    }
}
