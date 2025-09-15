package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.repo.BusinessProfileRepository;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import com.brandbuilder.reviewapp.service.BusinessProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class DebugController {

    @Autowired
    private BusinessProfileService businessProfileService;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping("/update-all-ratings")
    public ResponseEntity<?> updateAllRatings() {
        System.out.println("=== DEBUG: Manual rating update triggered ===");
        businessProfileService.updateAllBusinessRatings();
        return ResponseEntity.ok("All business ratings updated");
    }

    @GetMapping("/business-stats")
    public ResponseEntity<?> getBusinessStats() {
        List<BusinessProfile> businesses = businessProfileRepository.findAll();
        List<Review> reviews = reviewRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBusinesses", businesses.size());
        stats.put("totalReviews", reviews.size());

        // Detailed business info
        for (BusinessProfile business : businesses) {
            Map<String, Object> businessInfo = new HashMap<>();
            businessInfo.put("id", business.getId());
            businessInfo.put("name", business.getBusinessName());
            businessInfo.put("currentRating", business.getAverageRating());
            businessInfo.put("currentReviewCount", business.getTotalReviews());

            // Count actual reviews for this business
            Long actualReviewCount = reviewRepository.countReviewsByBusinessProfile(business);
            Double actualAvgRating = reviewRepository.findAverageRatingByBusinessProfile(business);

            businessInfo.put("actualReviewCount", actualReviewCount);
            businessInfo.put("actualAvgRating", actualAvgRating);

            stats.put("business_" + business.getId(), businessInfo);
        }

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/fix-business/{id}")
    public ResponseEntity<?> fixBusinessRating(@PathVariable Long id) {
        Optional<BusinessProfile> businessOpt = businessProfileRepository.findById(id);
        if (businessOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        BusinessProfile business = businessOpt.get();
        System.out.println("=== Fixing rating for business: " + business.getBusinessName() + " ===");

        businessProfileService.updateBusinessRating(business);

        // Return updated business
        BusinessProfile updated = businessProfileRepository.findById(id).get();
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();
        return ResponseEntity.ok(reviews);
    }
}