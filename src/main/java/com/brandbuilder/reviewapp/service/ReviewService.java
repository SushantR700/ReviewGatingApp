package com.brandbuilder.reviewapp.service;

import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.BusinessProfileRepository;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private BusinessProfileService businessProfileService;

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    public List<Review> getReviewsByBusinessProfile(Long businessProfileId) {
        Optional<BusinessProfile> businessProfile = businessProfileRepository.findById(businessProfileId);
        if (businessProfile.isPresent()) {
            return reviewRepository.findByBusinessProfile(businessProfile.get());
        }
        return List.of();
    }

    public List<Review> getReviewsByCustomer(User customer) {
        return reviewRepository.findByCustomer(customer);
    }

    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    public boolean hasCustomerReviewedBusiness(User customer, Long businessProfileId) {
        Optional<BusinessProfile> businessProfile = businessProfileRepository.findById(businessProfileId);
        if (businessProfile.isPresent()) {
            return reviewRepository.findByCustomerAndBusinessProfile(customer, businessProfile.get()).isPresent();
        }
        return false;
    }

    public Review createReview(Review review, User customer, Long businessProfileId) {
        Optional<BusinessProfile> businessProfileOpt = businessProfileRepository.findById(businessProfileId);

        if (businessProfileOpt.isEmpty()) {
            throw new RuntimeException("Business profile not found with id: " + businessProfileId);
        }

        BusinessProfile businessProfile = businessProfileOpt.get();

        // Check if customer has already reviewed this business
        if (hasCustomerReviewedBusiness(customer, businessProfileId)) {
            throw new RuntimeException("You have already reviewed this business");
        }

        review.setCustomer(customer);
        review.setBusinessProfile(businessProfile);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        // Set redirect flag based on rating
        if (review.getRating() > 3) {
            review.setRedirectedToGoogle(true);
        }

        Review savedReview = reviewRepository.save(review);

        // Update business profile rating
        businessProfileService.updateBusinessRating(businessProfile);

        return savedReview;
    }

    public Review updateReview(Long id, Review updatedReview, User customer) {
        Optional<Review> existingReviewOpt = reviewRepository.findById(id);

        if (existingReviewOpt.isEmpty()) {
            throw new RuntimeException("Review not found with id: " + id);
        }

        Review existingReview = existingReviewOpt.get();

        // Check if the customer owns this review
        if (!existingReview.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized to update this review");
        }

        existingReview.setRating(updatedReview.getRating());
        existingReview.setComment(updatedReview.getComment());
        existingReview.setUpdatedAt(LocalDateTime.now());

        // Update redirect flag based on new rating
        if (existingReview.getRating() > 3) {
            existingReview.setRedirectedToGoogle(true);
        } else {
            existingReview.setRedirectedToGoogle(false);
        }

        Review savedReview = reviewRepository.save(existingReview);

        // Update business profile rating
        businessProfileService.updateBusinessRating(existingReview.getBusinessProfile());

        return savedReview;
    }

    public void deleteReview(Long id, User customer) {
        Optional<Review> reviewOpt = reviewRepository.findById(id);

        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Review not found with id: " + id);
        }

        Review review = reviewOpt.get();

        // Check if the customer owns this review
        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized to delete this review");
        }

        BusinessProfile businessProfile = review.getBusinessProfile();
        reviewRepository.deleteById(id);

        // Update business profile rating after deletion
        businessProfileService.updateBusinessRating(businessProfile);
    }

    public List<Review> getLowRatingReviews() {
        return reviewRepository.findByRatingLessThanEqual(3);
    }
}