package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.BusinessProfile;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/businesses")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class BusinessProfileController {

    @Autowired
    private BusinessProfileService businessProfileService;

    @GetMapping
    public ResponseEntity<List<BusinessProfile>> getAllBusinessProfiles() {
        List<BusinessProfile> profiles = businessProfileService.getAllBusinessProfiles();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<BusinessProfile>> getTopRatedBusinessProfiles() {
        List<BusinessProfile> profiles = businessProfileService.getBusinessProfilesByRating();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessProfile> getBusinessProfileById(@PathVariable Long id) {
        Optional<BusinessProfile> profile = businessProfileService.getBusinessProfileById(id);
        return profile.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BusinessProfile>> searchBusinessProfiles(@RequestParam String name) {
        List<BusinessProfile> profiles = businessProfileService.searchBusinessProfiles(name);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getBusinessImage(@PathVariable Long id) {
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

