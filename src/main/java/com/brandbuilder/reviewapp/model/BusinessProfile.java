package com.brandbuilder.reviewapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "business_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Business name is required")
    @Column(nullable = false)
    private String businessName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Social Media Links
    private String facebookUrl;
    private String instagramUrl;
    private String twitterUrl;
    private String linkedinUrl;
    private String websiteUrl;

    // Google Review Link
    private String googleReviewUrl;

    // Image data
    private String imageName;
    private String imageType;

    @Lob
    @Column(name = "image_data", columnDefinition = "BYTEA")
    @JsonIgnore // Don't serialize image data in JSON responses
    private byte[] imageData;

    // Admin who created this profile - IGNORE THIS IN JSON TO PREVENT LAZY LOADING ISSUES
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore // This prevents the serialization error
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Reviews for this business - IGNORE THIS IN JSON TO PREVENT LAZY LOADING ISSUES
    @OneToMany(mappedBy = "businessProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // This prevents the serialization error and infinite recursion
    private List<Review> reviews;

    // Average rating calculation
    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;
}