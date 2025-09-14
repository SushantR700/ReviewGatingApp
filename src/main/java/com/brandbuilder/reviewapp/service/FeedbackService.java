package com.brandbuilder.reviewapp.service;

import com.brandbuilder.reviewapp.model.Feedback;
import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.repo.FeedbackRepository;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ReviewRepository reviewRepository;

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

    public Feedback createFeedback(Feedback feedback, Long reviewId) {
        Optional<Review> reviewOpt = reviewRepository.findById(reviewId);

        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Review not found with id: " + reviewId);
        }

        Review review = reviewOpt.get();

        // Check if feedback already exists for this review
        if (feedbackRepository.findByReview(review).isPresent()) {
            throw new RuntimeException("Feedback already exists for this review");
        }

        // Only allow feedback for low ratings (3 or less)
        if (review.getRating() > 3) {
            throw new RuntimeException("Feedback can only be provided for ratings of 3 or less");
        }

        feedback.setReview(review);
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setStatus(Feedback.FeedbackStatus.NEW);

        return feedbackRepository.save(feedback);
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