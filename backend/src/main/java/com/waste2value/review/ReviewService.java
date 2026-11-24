package com.waste2value.review;

import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.notification.NotificationService;
import com.waste2value.notification.NotificationType;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.review.dto.CreateReviewRequest;
import com.waste2value.review.dto.ReviewResponse;
import com.waste2value.review.dto.ReviewSummaryResponse;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PickupRequestRepository pickupRequestRepository;
    private final CollectorProfileRepository collectorProfileRepository;
    private final ProcessorProfileRepository processorProfileRepository;
    private final NotificationService notificationService;

    public ReviewService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            PickupRequestRepository pickupRequestRepository,
            CollectorProfileRepository collectorProfileRepository,
            ProcessorProfileRepository processorProfileRepository,
            NotificationService notificationService
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.pickupRequestRepository = pickupRequestRepository;
        this.collectorProfileRepository = collectorProfileRepository;
        this.processorProfileRepository = processorProfileRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReviewResponse submitReview(Long reviewerUserId, CreateReviewRequest request) {
        if (reviewerUserId.equals(request.targetUserId())) {
            throw new BadRequestException("You cannot submit a review for yourself");
        }

        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reviewerUserId));

        User targetUser = userRepository.findById(request.targetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + request.targetUserId()));

        PickupRequest pickupRequest = pickupRequestRepository.findById(request.pickupRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Pickup request not found with id: " + request.pickupRequestId()));

        if (pickupRequest.getStatus() != PickupRequestStatus.COMPLETED) {
            throw new BadRequestException("Reviews can only be submitted for COMPLETED pickups");
        }

        Long generatorId = pickupRequest.getListing().getGenerator().getId();
        Long collectorId = pickupRequest.getCollector().getId();

        boolean isEligibleReviewer = reviewerUserId.equals(generatorId) || reviewerUserId.equals(collectorId);
        if (!isEligibleReviewer) {
            throw new ForbiddenException("Only the generator or collector of this completed pickup can submit a review");
        }

        boolean isLegitimateTarget = (reviewerUserId.equals(generatorId) && request.targetUserId().equals(collectorId))
                || (reviewerUserId.equals(collectorId) && request.targetUserId().equals(generatorId));

        if (!isLegitimateTarget) {
            throw new BadRequestException("Target user must be the opposite participant of this completed pickup");
        }

        if (reviewRepository.existsByPickupRequestIdAndReviewerId(pickupRequest.getId(), reviewerUserId)) {
            throw new ConflictException("You have already submitted a review for this pickup request");
        }

        Review review = new Review();
        review.setReviewer(reviewer);
        review.setTargetUser(targetUser);
        review.setPickupRequest(pickupRequest);
        review.setRating(request.rating());
        review.setComment(request.comment());

        review = reviewRepository.save(review);

        // Update target profile average rating
        Double avgRatingDouble = reviewRepository.calculateAverageRatingForUser(targetUser.getId()).orElse(0.0);
        BigDecimal avgRating = BigDecimal.valueOf(avgRatingDouble).setScale(2, RoundingMode.HALF_UP);

        collectorProfileRepository.findByUserId(targetUser.getId()).ifPresent(collectorProfile -> {
            collectorProfile.setAvgRating(avgRating);
            collectorProfileRepository.save(collectorProfile);
        });

        processorProfileRepository.findByUserId(targetUser.getId()).ifPresent(processorProfile -> {
            processorProfile.setAvgRating(avgRating);
            processorProfileRepository.save(processorProfile);
        });

        // Notify target user
        notificationService.sendNotification(
                targetUser,
                NotificationType.REVIEW_RECEIVED,
                "New Rating & Review Received ⭐",
                reviewer.getFirstName() + " rated your transaction " + request.rating() + "/5 stars: \"" + (request.comment() != null ? request.comment() : "") + "\"",
                "REVIEW",
                review.getId()
        );

        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getUserReviews(Long targetUserId, Pageable pageable) {
        Double avgRatingDouble = reviewRepository.calculateAverageRatingForUser(targetUserId).orElse(0.0);
        BigDecimal avgRating = BigDecimal.valueOf(avgRatingDouble).setScale(2, RoundingMode.HALF_UP);
        long totalCount = reviewRepository.countByTargetUserId(targetUserId);

        PageResponse<ReviewResponse> reviewsPage = PageResponse.from(
                reviewRepository.findByTargetUserId(targetUserId, pageable).map(ReviewResponse::from)
        );

        return new ReviewSummaryResponse(targetUserId, avgRating, totalCount, reviewsPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMySubmittedReviews(Long reviewerUserId, Pageable pageable) {
        return PageResponse.from(
                reviewRepository.findByReviewerId(reviewerUserId, pageable).map(ReviewResponse::from)
        );
    }
}
