package com.waste2value.collector;

import com.waste2value.collector.dto.*;
import com.waste2value.notification.NotificationService;
import com.waste2value.notification.NotificationType;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CollectorProfileService {

    private final CollectorProfileRepository collectorProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CollectorProfileService(
            CollectorProfileRepository collectorProfileRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.collectorProfileRepository = collectorProfileRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CollectorProfileResponse createOrUpdateProfile(Long userId, CollectorProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.COLLECTOR) {
            throw new ForbiddenException("Only users with role COLLECTOR can have a collector profile");
        }

        CollectorProfile profile = collectorProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CollectorProfile newProfile = new CollectorProfile();
                    newProfile.setUser(user);
                    newProfile.setVerified(false);
                    return newProfile;
                });

        profile.setVehicleType(request.vehicleType());
        profile.setServiceArea(request.serviceArea());
        profile.setMaxDistanceKm(request.maxDistanceKm());

        profile = collectorProfileRepository.save(profile);
        return CollectorProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public CollectorProfileResponse getMyProfile(Long userId) {
        CollectorProfile profile = collectorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector profile not found for current user"));
        return CollectorProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public CollectorPublicResponse getPublicCollector(Long id) {
        CollectorProfile profile = collectorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collector not found with id: " + id));
        return CollectorPublicResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public PageResponse<CollectorPublicResponse> getPublicCollectors(Pageable pageable) {
        return PageResponse.from(
                collectorProfileRepository.findByIsVerifiedTrue(pageable)
                        .map(CollectorPublicResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CollectorProfileResponse> getPendingCollectors(Pageable pageable) {
        return PageResponse.from(
                collectorProfileRepository.findByIsVerifiedFalse(pageable)
                        .map(CollectorProfileResponse::from)
        );
    }

    @Transactional
    public CollectorProfileResponse verifyCollector(Long adminId, Long profileId, boolean verified, String note) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        CollectorProfile profile = collectorProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector profile not found with id: " + profileId));

        profile.setVerified(verified);
        profile.setVerifiedBy(verified ? admin : null);
        profile.setVerifiedAt(verified ? Instant.now() : null);

        profile = collectorProfileRepository.save(profile);

        // Notify collector
        NotificationType type = verified ? NotificationType.PROFILE_VERIFIED : NotificationType.PROFILE_REJECTED;
        String title = verified ? "Collector Profile Verified!" : "Collector Profile Verification Suspended";
        String message = verified
                ? "Your collector profile has been verified by the Waste2Value platform administrator. You can now request waste pickups!"
                : "Your collector profile verification has been updated/suspended: " + (note != null ? note : "Contact admin for details.");

        notificationService.sendNotification(profile.getUser(), type, title, message, "COLLECTOR_PROFILE", profile.getId());

        return CollectorProfileResponse.from(profile);
    }
}
