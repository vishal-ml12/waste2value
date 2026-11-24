package com.waste2value.review;

import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.listing.Listing;
import com.waste2value.notification.NotificationService;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.processor.ProcessorProfileRepository;
import com.waste2value.review.dto.CreateReviewRequest;
import com.waste2value.review.dto.ReviewResponse;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PickupRequestRepository pickupRequestRepository;

    @Mock
    private CollectorProfileRepository collectorProfileRepository;

    @Mock
    private ProcessorProfileRepository processorProfileRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewService reviewService;

    private User generator;
    private User collector;
    private PickupRequest completedPickup;

    @BeforeEach
    void setUp() {
        generator = new User();
        generator.setId(2L);
        generator.setRole(Role.GENERATOR);
        generator.setFirstName("Eco");
        generator.setLastName("Generator");

        collector = new User();
        collector.setId(3L);
        collector.setRole(Role.COLLECTOR);
        collector.setFirstName("City");
        collector.setLastName("Collector");

        Listing listing = new Listing();
        listing.setId(10L);
        listing.setGenerator(generator);

        completedPickup = new PickupRequest();
        completedPickup.setId(100L);
        completedPickup.setListing(listing);
        completedPickup.setCollector(collector);
        completedPickup.setStatus(PickupRequestStatus.COMPLETED);
    }

    @Test
    @DisplayName("Self-review is strictly rejected")
    void selfReviewIsRejected() {
        CreateReviewRequest req = new CreateReviewRequest(100L, 2L, 5, "I am great");

        assertThatThrownBy(() -> reviewService.submitReview(2L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot submit a review for yourself");
    }

    @Test
    @DisplayName("Review on non-completed pickup is rejected")
    void nonCompletedPickupReviewRejected() {
        completedPickup.setStatus(PickupRequestStatus.ACCEPTED);

        when(userRepository.findById(2L)).thenReturn(Optional.of(generator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(collector));
        when(pickupRequestRepository.findById(100L)).thenReturn(Optional.of(completedPickup));

        CreateReviewRequest req = new CreateReviewRequest(100L, 3L, 5, "Great job");

        assertThatThrownBy(() -> reviewService.submitReview(2L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED pickups");
    }

    @Test
    @DisplayName("Duplicate review for same pickup and reviewer is rejected")
    void duplicateReviewRejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(generator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(collector));
        when(pickupRequestRepository.findById(100L)).thenReturn(Optional.of(completedPickup));
        when(reviewRepository.existsByPickupRequestIdAndReviewerId(100L, 2L)).thenReturn(true);

        CreateReviewRequest req = new CreateReviewRequest(100L, 3L, 5, "Great job again");

        assertThatThrownBy(() -> reviewService.submitReview(2L, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already submitted a review");
    }

    @Test
    @DisplayName("Valid review creates review and recalculates average rating for target profile")
    void validReviewUpdatesAvgRating() {
        CollectorProfile collectorProfile = new CollectorProfile();
        collectorProfile.setId(1L);
        collectorProfile.setUser(collector);

        when(userRepository.findById(2L)).thenReturn(Optional.of(generator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(collector));
        when(pickupRequestRepository.findById(100L)).thenReturn(Optional.of(completedPickup));
        when(reviewRepository.existsByPickupRequestIdAndReviewerId(100L, 2L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            return r;
        });
        when(reviewRepository.calculateAverageRatingForUser(3L)).thenReturn(Optional.of(4.8));
        when(collectorProfileRepository.findByUserId(3L)).thenReturn(Optional.of(collectorProfile));

        CreateReviewRequest req = new CreateReviewRequest(100L, 3L, 5, "Punctual and courteous!");
        ReviewResponse resp = reviewService.submitReview(2L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.rating()).isEqualTo(5);
        assertThat(collectorProfile.getAvgRating()).isEqualTo(new BigDecimal("4.80"));
        verify(notificationService, times(1)).sendNotification(eq(collector), any(), anyString(), anyString(), anyString(), any());
    }
}
