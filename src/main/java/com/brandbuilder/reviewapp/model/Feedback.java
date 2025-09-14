package com.brandbuilder.reviewapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @Column(columnDefinition = "TEXT")
    private String feedbackText;

    // Specific feedback categories
    private String serviceQuality;
    private String staffBehavior;
    private String cleanliness;
    private String valueForMoney;
    private String overallExperience;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    // Contact information if customer wants follow-up
    private String contactEmail;
    private String contactPhone;

    @Column(name = "wants_followup")
    private Boolean wantsFollowup = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Status of feedback handling
    @Enumerated(EnumType.STRING)
    private FeedbackStatus status = FeedbackStatus.NEW;

    @Column(name = "admin_response")
    private String adminResponse;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public enum FeedbackStatus {
        NEW, IN_PROGRESS, RESOLVED, CLOSED
    }
}