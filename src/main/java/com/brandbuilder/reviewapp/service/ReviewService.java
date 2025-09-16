package com.brandbuilder.reviewapp.service;

import com.brandbuilder.reviewapp.controller.ReviewController;
import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.BusinessProfileRepository;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
    private EntityManager entityManager;

    // REMOVED EmailService - NO EMAIL SENT FROM REVIEWS

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

    public List<Review> getReviewsForBusinessOwner(Long businessProfileId, User businessOwner) {
        System.out.println("=== Getting reviews for business owner ===");
        System.out.println("Business ID: " + businessProfileId);
        System.out.println("Owner: " + businessOwner.getName());

        Optional<BusinessProfile> businessProfileOpt = businessProfileRepository.findById(businessProfileId);
        if (businessProfileOpt.isEmpty()) {
            throw new RuntimeException("Business profile not found with id: " + businessProfileId);
        }

        BusinessProfile businessProfile = businessProfileOpt.get();

        // Verify that this business owner actually owns this business
        if (!businessProfile.getCreatedBy().getId().equals(businessOwner.getId())) {
            System.out.println("Business owner verification failed:");
            System.out.println("Business created by: " + businessProfile.getCreatedBy().getName() + " (ID: " + businessProfile.getCreatedBy().getId() + ")");
            System.out.println("Current user: " + businessOwner.getName() + " (ID: " + businessOwner.getId() + ")");
            throw new RuntimeException("Unauthorized: You can only view reviews for your own businesses");
        }

        System.out.println("Ownership verified. Fetching reviews...");
        List<Review> reviews = reviewRepository.findByBusinessProfile(businessProfile);
        System.out.println("Found " + reviews.size() + " reviews");

        return reviews;
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

    // FIXED: Create anonymous review WITHOUT any email
    @Transactional
    public Review createAnonymousReview(ReviewController.AnonymousReviewRequest request, Long businessProfileId) {
        System.out.println("=== Creating Anonymous Review ===");
        System.out.println("Business ID: " + businessProfileId);
        System.out.println("Anonymous: " + request.getIsAnonymous());
        System.out.println("Rating: " + request.getRating());

        Optional<BusinessProfile> businessProfileOpt = businessProfileRepository.findById(businessProfileId);

        if (businessProfileOpt.isEmpty()) {
            throw new RuntimeException("Business profile not found with id: " + businessProfileId);
        }

        BusinessProfile businessProfile = businessProfileOpt.get();

        // Create review
        Review review = new Review();
        review.setRating(request.getRating());
        review.setComment(request.getComment() != null ? request.getComment().trim() : "");
        review.setBusinessProfile(businessProfile);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        review.setIsAnonymous(request.getIsAnonymous());

        // Set customer info based on anonymous flag
        if (request.getIsAnonymous()) {
            review.setCustomer(null);
            review.setCustomerName("");
            review.setCustomerEmail("");
            review.setCustomerPhone("");
        } else {
            review.setCustomer(null); // Still null since no user account
            review.setCustomerName(request.getCustomerName() != null ? request.getCustomerName().trim() : "");
            review.setCustomerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : "");
            review.setCustomerPhone(request.getCustomerPhone() != null ? request.getCustomerPhone().trim() : "");
        }

        // Set redirect flag based on rating
        if (review.getRating() > 3) {
            review.setRedirectedToGoogle(true);
        }

        // Save the review first
        Review savedReview = reviewRepository.save(review);

        // Force flush to ensure review is saved before updating business rating
        entityManager.flush();

        // Update business profile rating using native query to avoid validation issues
        updateBusinessRatingDirectly(businessProfile);

        // NO EMAIL SENT HERE - emails will be sent only when feedback is complete
        System.out.println("Review saved. NO email sent - waiting for feedback completion.");

        return savedReview;
    }

    // FIXED: Create review WITHOUT any email
    @Transactional
    public Review createReview(Review review, User customer, Long businessProfileId) {
        System.out.println("=== Creating Authenticated User Review ===");
        System.out.println("Business ID: " + businessProfileId);
        System.out.println("Customer: " + customer.getName());
        System.out.println("Rating: " + review.getRating());

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

        // Set customer info from user account
        review.setCustomerName(customer.getName());
        review.setCustomerEmail(customer.getEmail());
        review.setIsAnonymous(false);

        // Set redirect flag based on rating
        if (review.getRating() > 3) {
            review.setRedirectedToGoogle(true);
        }

        // Save the review first
        Review savedReview = reviewRepository.save(review);

        // Force flush to ensure review is saved before updating business rating
        entityManager.flush();

        // Update business profile rating using native query to avoid validation issues
        updateBusinessRatingDirectly(businessProfile);

        // NO EMAIL SENT HERE - emails will be sent only when feedback is complete
        System.out.println("Review saved. NO email sent - waiting for feedback completion.");

        return savedReview;
    }

    @Transactional
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
        entityManager.flush();

        // Update business profile rating using native query
        updateBusinessRatingDirectly(existingReview.getBusinessProfile());

        return savedReview;
    }

    @Transactional
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
        entityManager.flush();

        // Update business profile rating after deletion
        updateBusinessRatingDirectly(businessProfile);
    }

    public List<Review> getLowRatingReviews() {
        return reviewRepository.findByRatingLessThanEqual(3);
    }

    // Direct SQL update to avoid validation issues
    @Transactional
    private void updateBusinessRatingDirectly(BusinessProfile businessProfile) {
        try {
            System.out.println("=== Direct rating update for business: " + businessProfile.getBusinessName() + " (ID: " + businessProfile.getId() + ") ===");

            // Calculate rating using repository methods
            Double avgRating = reviewRepository.findAverageRatingByBusinessProfile(businessProfile);
            Long totalReviews = reviewRepository.countReviewsByBusinessProfile(businessProfile);

            System.out.println("Raw query results - avgRating: " + avgRating + ", totalReviews: " + totalReviews);

            // Handle null values properly
            double finalRating = (avgRating != null) ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
            int finalCount = (totalReviews != null) ? totalReviews.intValue() : 0;

            System.out.println("Calculated values - finalRating: " + finalRating + ", finalCount: " + finalCount);

            // Use native SQL to update only the rating fields directly
            Query updateQuery = entityManager.createNativeQuery(
                    "UPDATE business_profiles SET average_rating = ?, total_reviews = ?, updated_at = ? WHERE id = ?"
            );
            updateQuery.setParameter(1, finalRating);
            updateQuery.setParameter(2, finalCount);
            updateQuery.setParameter(3, LocalDateTime.now());
            updateQuery.setParameter(4, businessProfile.getId());

            int updatedRows = updateQuery.executeUpdate();

            if (updatedRows > 0) {
                System.out.println("✅ Business rating updated successfully using native SQL:");
                System.out.println("  - Business: " + businessProfile.getBusinessName());
                System.out.println("  - Average Rating: " + finalRating);
                System.out.println("  - Total Reviews: " + finalCount);
                System.out.println("  - Rows Updated: " + updatedRows);

                // Update the in-memory object as well
                businessProfile.setAverageRating(finalRating);
                businessProfile.setTotalReviews(finalCount);
                businessProfile.setUpdatedAt(LocalDateTime.now());
            } else {
                System.out.println("❌ No rows updated for business ID: " + businessProfile.getId());
            }

        } catch (Exception e) {
            System.err.println("❌ Error updating business rating for " + businessProfile.getBusinessName() + ": " + e.getMessage());
            e.printStackTrace();
            // Don't rethrow the exception to prevent transaction rollback
        }
    }
}