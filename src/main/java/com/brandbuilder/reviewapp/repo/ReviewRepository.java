package com.brandbuilder.reviewapp.repo;
import com.brandbuilder.reviewapp.model.BusinessProfile;

import com.brandbuilder.reviewapp.model.Review;
import com.brandbuilder.reviewapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBusinessProfile(BusinessProfile businessProfile);
    List<Review> findByCustomer(User customer);
    Optional<Review> findByCustomerAndBusinessProfile(User customer, BusinessProfile businessProfile);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.businessProfile = :businessProfile")
    Double findAverageRatingByBusinessProfile(@Param("businessProfile") BusinessProfile businessProfile);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.businessProfile = :businessProfile")
    Long countReviewsByBusinessProfile(@Param("businessProfile") BusinessProfile businessProfile);

    List<Review> findByRatingLessThanEqual(Integer rating);
}
