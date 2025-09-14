package com.brandbuilder.reviewapp.service;

import com.brandbuilder.reviewapp.model.BusinessProfile;

import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.BusinessProfileRepository;
import com.brandbuilder.reviewapp.repo.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public List<BusinessProfile> getAllBusinessProfiles() {
        return businessProfileRepository.findAll();
    }

    public List<BusinessProfile> getBusinessProfilesByRating() {
        return businessProfileRepository.findAllOrderByRating();
    }

    public Optional<BusinessProfile> getBusinessProfileById(Long id) {
        return businessProfileRepository.findById(id);
    }

    public List<BusinessProfile> searchBusinessProfiles(String businessName) {
        return businessProfileRepository.findByBusinessNameContainingIgnoreCase(businessName);
    }

    public List<BusinessProfile> getBusinessProfilesByAdmin(User admin) {
        return businessProfileRepository.findByCreatedBy(admin);
    }

    public BusinessProfile createBusinessProfile(BusinessProfile profile, MultipartFile image, User admin) throws IOException {
        if (image != null && !image.isEmpty()) {
            profile.setImageName(image.getOriginalFilename());
            profile.setImageType(image.getContentType());
            profile.setImageData(image.getBytes());
        }

        profile.setCreatedBy(admin);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());

        return businessProfileRepository.save(profile);
    }

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

    public void updateBusinessRating(BusinessProfile businessProfile) {
        Double avgRating = reviewRepository.findAverageRatingByBusinessProfile(businessProfile);
        Long totalReviews = reviewRepository.countReviewsByBusinessProfile(businessProfile);

        businessProfile.setAverageRating(avgRating != null ? avgRating : 0.0);
        businessProfile.setTotalReviews(totalReviews.intValue());

        businessProfileRepository.save(businessProfile);
    }
}