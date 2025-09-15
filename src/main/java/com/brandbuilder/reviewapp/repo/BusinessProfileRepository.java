package com.brandbuilder.reviewapp.repo;

import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    List<BusinessProfile> findByCreatedBy(User user);

    @Query("SELECT bp FROM BusinessProfile bp ORDER BY bp.averageRating DESC")
    List<BusinessProfile> findAllOrderByRating();

    List<BusinessProfile> findByBusinessNameContainingIgnoreCase(String businessName);

    // Direct SQL update to avoid validation issues when updating ratings
    @Modifying
    @Transactional
    @Query("UPDATE BusinessProfile bp SET bp.averageRating = :avgRating, bp.totalReviews = :totalReviews WHERE bp.id = :businessId")
    void updateBusinessRating(@Param("businessId") Long businessId,
                              @Param("avgRating") Double avgRating,
                              @Param("totalReviews") Integer totalReviews);
}