package com.brandbuilder.reviewapp.service;

import com.brandbuilder.reviewapp.model.Feedback;
import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.repo.FeedbackRepository;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    // REMOVED: EmailService - no longer sending emails from feedback creation

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    public List<Feedback> getFeedbackByStatus(Feedback.FeedbackStatus status) {
        return feedbackRepository.findByStatus(status);
    }

    public List<Feedback> getFeedbackRequiringFollowup() {
        return feedbackRepository.findByWantsFollowup(true);
    }

    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }

    public Optional<Feedback> getFeedbackByReview(Review review) {
        return feedbackRepository.findByReview(review);
    }

    @Transactional
    public Feedback createFeedback(Feedback feedback, Long reviewId) {
        System.out.println("=== Creating Feedback ===");
        System.out.println("Review ID: " + reviewId);

        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);

        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Review not found with id: " + reviewId);
        }

        Review review = reviewOpt.get();
        System.out.println("Found review with rating: " + review.getRating());

        // Check if feedback already exists for this review
        Optional<Feedback> existingFeedback = feedbackRepository.findByReview(review);
        if (existingFeedback.isPresent()) {
            System.out.println("Feedback already exists for review ID: " + reviewId);
            throw new RuntimeException("Feedback already exists for this review");
        }

        // Only allow feedback for low ratings (3 or less)
        if (review.getRating() > 3) {
            throw new RuntimeException("Feedback can only be provided for ratings of 3 or less");
        }

        feedback.setReview(review);
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setStatus(Feedback.FeedbackStatus.NEW);

        System.out.println("Saving feedback...");
        // Save feedback
        Feedback savedFeedback = feedbackRepository.save(feedback);
        System.out.println("✅ Feedback saved with ID: " + savedFeedback.getId());

        // REMOVED: Email sending - emails are now sent when the review is initially submitted
        System.out.println("📧 Email was already sent when the review was initially submitted. No additional email sent for feedback.");

        return savedFeedback;
    }

    public Feedback updateFeedbackStatus(Long id, Feedback.FeedbackStatus status, String adminResponse) {
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(id);

        if (feedbackOpt.isEmpty()) {
            throw new RuntimeException("Feedback not found with id: " + id);
        }

        Feedback feedback = feedbackOpt.get();
        feedback.setStatus(status);

        if (adminResponse != null && !adminResponse.trim().isEmpty()) {
            feedback.setAdminResponse(adminResponse);
            feedback.setRespondedAt(LocalDateTime.now());
        }

        return feedbackRepository.save(feedback);
    }

    public void deleteFeedback(Long id) {
        if (!feedbackRepository.existsById(id)) {
            throw new RuntimeException("Feedback not found with id: " + id);
        }

        feedbackRepository.deleteById(id);
    }

    public List<Feedback> getNewFeedback() {
        return feedbackRepository.findByStatus(Feedback.FeedbackStatus.NEW);
    }
}