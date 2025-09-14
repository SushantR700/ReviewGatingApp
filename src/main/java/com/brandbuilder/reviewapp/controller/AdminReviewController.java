package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Admin Controller for Reviews
@RestController
@RequestMapping("/api/admin/reviews")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
class AdminReviewController {

    @Autowired
    private ReviewService reviewService;

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
}
