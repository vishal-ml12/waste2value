package com.waste2value.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByTargetUserId(Long targetUserId, Pageable pageable);

    Page<Review> findByReviewerId(Long reviewerId, Pageable pageable);

    boolean existsByPickupRequestIdAndReviewerId(Long pickupRequestId, Long reviewerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.targetUser.id = :targetUserId")
    Optional<Double> calculateAverageRatingForUser(@Param("targetUserId") Long targetUserId);

    long countByTargetUserId(Long targetUserId);
}
