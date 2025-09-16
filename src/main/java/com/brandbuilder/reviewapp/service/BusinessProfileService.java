package com.brandbuilder.reviewapp.service;

import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.BusinessProfileRepository;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BusinessProfileService {

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    // Helper method to create URL-friendly business name slug
    private String createBusinessSlug(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            return "";
        }

        return businessName
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters except spaces and hyphens
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single hyphen
                .trim()
                .replaceAll("^-+|-+$", ""); // Remove leading/trailing hyphens
    }

    // Helper method to match business name slug
    private boolean matchesBusinessSlug(BusinessProfile business, String searchSlug) {
        if (business == null || business.getBusinessName() == null || searchSlug == null) {
            return false;
        }

        String businessSlug = createBusinessSlug(business.getBusinessName());
        return businessSlug.equals(searchSlug.toLowerCase());
    }

    // Simplified methods without automatic rating updates to prevent issues
    public List<BusinessProfile> getAllBusinessProfiles() {
        return businessProfileRepository.findAll();
    }

    public List<BusinessProfile> getBusinessProfilesByRating() {
        return businessProfileRepository.findAllOrderByRating();
    }

    public Optional<BusinessProfile> getBusinessProfileById(Long id) {
        return businessProfileRepository.findById(id);
    }

    // NEW: Get business profile by name slug
    public Optional<BusinessProfile> getBusinessProfileByNameSlug(String businessNameSlug) {
        System.out.println("=== Looking for business with slug: " + businessNameSlug + " ===");

        if (businessNameSlug == null || businessNameSlug.trim().isEmpty()) {
            System.out.println("Empty slug provided");
            return Optional.empty();
        }

        // Get all businesses and find the one that matches the slug
        List<BusinessProfile> allBusinesses = businessProfileRepository.findAll();

        for (BusinessProfile business : allBusinesses) {
            String businessSlug = createBusinessSlug(business.getBusinessName());
            System.out.println("Checking business: " + business.getBusinessName() + " -> slug: " + businessSlug);

            if (businessSlug.equals(businessNameSlug.toLowerCase())) {
                System.out.println("Found matching business: " + business.getBusinessName());
                return Optional.of(business);
            }
        }

        System.out.println("No business found for slug: " + businessNameSlug);
        return Optional.empty();
    }

    public List<BusinessProfile> searchBusinessProfiles(String businessName) {
        return businessProfileRepository.findByBusinessNameContainingIgnoreCase(businessName);
    }

    public List<BusinessProfile> getBusinessProfilesByAdmin(User admin) {
        return businessProfileRepository.findByCreatedBy(admin);
    }

    @Transactional
    public BusinessProfile createBusinessProfile(BusinessProfile profile, MultipartFile image, User admin) throws IOException {
        if (image != null && !image.isEmpty()) {
            profile.setImageName(image.getOriginalFilename());
            profile.setImageType(image.getContentType());
            profile.setImageData(image.getBytes());
        }

        profile.setCreatedBy(admin);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setAverageRating(0.0);
        profile.setTotalReviews(0);

        return businessProfileRepository.save(profile);
    }

    @Transactional
    public BusinessProfile updateBusinessProfile(Long id, BusinessProfile updatedProfile, MultipartFile image, User admin) throws IOException {
        Optional<BusinessProfile> existingProfileOpt = businessProfileRepository.findById(id);

        if (existingProfileOpt.isEmpty()) {
            throw new RuntimeException("Business profile not found with id: " + id);
        }

        BusinessProfile existingProfile = existingProfileOpt.get();

        // Check if the admin owns this profile
        if (!existingProfile.getCreatedBy().getId().equals(admin.getId())) {
            throw new RuntimeException("Unauthorized to update this business profile");
        }

        // Update fields
        existingProfile.setBusinessName(updatedProfile.getBusinessName());
        existingProfile.setPhoneNumber(updatedProfile.getPhoneNumber());
        existingProfile.setAddress(updatedProfile.getAddress());
        existingProfile.setDescription(updatedProfile.getDescription());
        existingProfile.setFacebookUrl(updatedProfile.getFacebookUrl());
        existingProfile.setInstagramUrl(updatedProfile.getInstagramUrl());
        existingProfile.setTwitterUrl(updatedProfile.getTwitterUrl());
        existingProfile.setLinkedinUrl(updatedProfile.getLinkedinUrl());
        existingProfile.setWebsiteUrl(updatedProfile.getWebsiteUrl());
        existingProfile.setGoogleReviewUrl(updatedProfile.getGoogleReviewUrl());

        if (image != null && !image.isEmpty()) {
            existingProfile.setImageName(image.getOriginalFilename());
            existingProfile.setImageType(image.getContentType());
            existingProfile.setImageData(image.getBytes());
        }

        existingProfile.setUpdatedAt(LocalDateTime.now());

        return businessProfileRepository.save(existingProfile);
    }

    @Transactional
    public void deleteBusinessProfile(Long id, User admin) {
        Optional<BusinessProfile> profileOpt = businessProfileRepository.findById(id);

        if (profileOpt.isEmpty()) {
            throw new RuntimeException("Business profile not found with id: " + id);
        }

        BusinessProfile profile = profileOpt.get();

        // Check if the admin owns this profile
        if (!profile.getCreatedBy().getId().equals(admin.getId())) {
            throw new RuntimeException("Unauthorized to delete this business profile");
        }

        businessProfileRepository.deleteById(id);
    }

    // Manual rating update method - call this explicitly when needed
    @Transactional
    public void updateBusinessRating(BusinessProfile businessProfile) {
        try {
            System.out.println("=== Manual rating update for business: " + businessProfile.getBusinessName() + " ===");

            Double avgRating = reviewRepository.findAverageRatingByBusinessProfile(businessProfile);
            Long totalReviews = reviewRepository.countReviewsByBusinessProfile(businessProfile);

            double finalRating = (avgRating != null) ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
            int finalCount = (totalReviews != null) ? totalReviews.intValue() : 0;

            businessProfile.setAverageRating(finalRating);
            businessProfile.setTotalReviews(finalCount);
            businessProfile.setUpdatedAt(LocalDateTime.now());

            businessProfileRepository.save(businessProfile);

            System.out.println("✅ Rating updated: " + finalRating + " (" + finalCount + " reviews)");

        } catch (Exception e) {
            System.err.println("❌ Error updating rating: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method for DebugController compatibility
    @Transactional
    public void updateAllBusinessRatings() {
        List<BusinessProfile> allProfiles = businessProfileRepository.findAll();
        System.out.println("=== Updating ratings for all " + allProfiles.size() + " businesses ===");

        for (BusinessProfile profile : allProfiles) {
            updateBusinessRating(profile);
        }

        System.out.println("=== Finished updating all business ratings ===");
    }
}