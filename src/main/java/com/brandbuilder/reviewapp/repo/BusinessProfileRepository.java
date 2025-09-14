package com.brandbuilder.reviewapp.repo;
import com.brandbuilder.reviewapp.model.BusinessProfile;
import com.brandbuilder.reviewapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    List<BusinessProfile> findByCreatedBy(User user);

    @Query("SELECT bp FROM BusinessProfile bp ORDER BY bp.averageRating DESC")
    List<BusinessProfile> findAllOrderByRating();

    List<BusinessProfile> findByBusinessNameContainingIgnoreCase(String businessName);
}
