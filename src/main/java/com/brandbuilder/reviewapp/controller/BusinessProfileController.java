package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.service.BusinessProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/businesses")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class BusinessProfileController {

    @Autowired
    private BusinessProfileService businessProfileService;

    @GetMapping
    public ResponseEntity<List<BusinessProfile>> getAllBusinessProfiles() {
        System.out.println("GET /api/businesses - called");
        try {
            List<BusinessProfile> profiles = businessProfileService.getAllBusinessProfiles();
            System.out.println("Found " + profiles.size() + " business profiles");
            return ResponseEntity.ok(profiles);
        } catch (Exception e) {
            System.err.println("Error in getAllBusinessProfiles: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<BusinessProfile>> getTopRatedBusinessProfiles() {
        System.out.println("GET /api/businesses/top-rated - called");
        List<BusinessProfile> profiles = businessProfileService.getBusinessProfilesByRating();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessProfile> getBusinessProfileById(@PathVariable Long id) {
        System.out.println("GET /api/businesses/" + id + " - called");
        Optional<BusinessProfile> profile = businessProfileService.getBusinessProfileById(id);
        return profile.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BusinessProfile>> searchBusinessProfiles(@RequestParam String name) {
        System.out.println("GET /api/businesses/search?name=" + name + " - called");
        List<BusinessProfile> profiles = businessProfileService.searchBusinessProfiles(name);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getBusinessImage(@PathVariable Long id) {
        System.out.println("GET /api/businesses/" + id + "/image - called");
        Optional<BusinessProfile> profileOpt = businessProfileService.getBusinessProfileById(id);

        if (profileOpt.isPresent() && profileOpt.get().getImageData() != null) {
            BusinessProfile profile = profileOpt.get();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(profile.getImageType()))
                    .body(profile.getImageData());
        }

        return ResponseEntity.notFound().build();
    }
}