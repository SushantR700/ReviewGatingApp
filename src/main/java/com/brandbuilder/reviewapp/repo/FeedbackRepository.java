package com.brandbuilder.reviewapp.repo;
import com.brandbuilder.reviewapp.model.Feedback;
import com.brandbuilder.reviewapp.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findByReview(Review review);
    List<Feedback> findByStatus(Feedback.FeedbackStatus status);
    List<Feedback> findByWantsFollowup(Boolean wantsFollowup);
}
