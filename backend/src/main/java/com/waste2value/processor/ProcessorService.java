package com.waste2value.processor;

import com.waste2value.category.Category;
import com.waste2value.category.CategoryRepository;
import com.waste2value.notification.NotificationService;
import com.waste2value.notification.NotificationType;
import com.waste2value.processor.dto.*;
import com.waste2value.shared.PageResponse;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class ProcessorService {

    private final ProcessorProfileRepository processorProfileRepository;
    private final ProcessorCapabilityRepository processorCapabilityRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ProcessorService(
            ProcessorProfileRepository processorProfileRepository,
            ProcessorCapabilityRepository processorCapabilityRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.processorProfileRepository = processorProfileRepository;
        this.processorCapabilityRepository = processorCapabilityRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProcessorProfileResponse createOrUpdateProfile(Long userId, ProcessorProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.PROCESSOR) {
            throw new ForbiddenException("Only users with role PROCESSOR can have a processor profile");
        }

        ProcessorProfile profile = processorProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ProcessorProfile newProfile = new ProcessorProfile();
                    newProfile.setUser(user);
                    newProfile.setVerified(false);
                    return newProfile;
                });

        if (request.maxCapacityKg().compareTo(profile.getCurrentLoadKg()) < 0) {
            throw new BadRequestException("Maximum capacity cannot be less than current load: " + profile.getCurrentLoadKg() + " kg");
        }

        profile.setCompanyName(request.companyName());
        profile.setLicenseNumber(request.licenseNumber());
        profile.setDescription(request.description());
        profile.setMaxCapacityKg(request.maxCapacityKg());

        profile = processorProfileRepository.save(profile);
        return ProcessorProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public ProcessorProfileResponse getMyProfile(Long userId) {
        ProcessorProfile profile = processorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));
        return ProcessorProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public ProcessorPublicResponse getPublicProcessor(Long id) {
        ProcessorProfile profile = processorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Processor not found with id: " + id));
        return ProcessorPublicResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProcessorPublicResponse> getPublicProcessors(Pageable pageable) {
        return PageResponse.from(
                processorProfileRepository.findByIsVerifiedTrue(pageable)
                        .map(ProcessorPublicResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProcessorProfileResponse> getPendingProcessors(Pageable pageable) {
        return PageResponse.from(
                processorProfileRepository.findByIsVerifiedFalse(pageable)
                        .map(ProcessorProfileResponse::from)
        );
    }

    @Transactional
    public ProcessorProfileResponse verifyProcessor(Long adminId, Long profileId, boolean verified, String note) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        ProcessorProfile profile = processorProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found with id: " + profileId));

        profile.setVerified(verified);
        profile.setVerifiedBy(verified ? admin : null);
        profile.setVerifiedAt(verified ? Instant.now() : null);

        profile = processorProfileRepository.save(profile);

        NotificationType type = verified ? NotificationType.PROFILE_VERIFIED : NotificationType.PROFILE_REJECTED;
        String title = verified ? "Processor Profile Verified!" : "Processor Profile Verification Suspended";
        String message = verified
                ? "Your processor facility profile has been verified by the Waste2Value administrator. You can now accept incoming material receipts!"
                : "Your processor profile verification has been updated/suspended: " + (note != null ? note : "Contact admin for details.");

        notificationService.sendNotification(profile.getUser(), type, title, message, "PROCESSOR_PROFILE", profile.getId());

        return ProcessorProfileResponse.from(profile);
    }

    // CAPABILITY MANAGEMENT
    @Transactional(readOnly = true)
    public List<ProcessorCapabilityResponse> getMyCapabilities(Long userId) {
        ProcessorProfile profile = processorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        return processorCapabilityRepository.findByProcessorId(profile.getId())
                .stream()
                .map(ProcessorCapabilityResponse::from)
                .toList();
    }

    @Transactional
    public ProcessorCapabilityResponse addCapability(Long userId, ProcessorCapabilityRequest request) {
        ProcessorProfile profile = processorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        if (request.minQuantity().compareTo(request.maxQuantity()) > 0) {
            throw new BadRequestException("Minimum quantity cannot exceed maximum quantity");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        if (!category.isActive()) {
            throw new BadRequestException("Cannot add capability for an inactive category");
        }

        if (processorCapabilityRepository.existsByProcessorIdAndCategoryId(profile.getId(), category.getId())) {
            throw new ConflictException("Capability for category '" + category.getName() + "' already exists for this processor");
        }

        ProcessorCapability capability = new ProcessorCapability();
        capability.setProcessor(profile);
        capability.setCategory(category);
        capability.setMinQuantity(request.minQuantity());
        capability.setMaxQuantity(request.maxQuantity());
        capability.setPricePerUnit(request.pricePerUnit());
        capability.setActive(request.isActive() == null || request.isActive());

        capability = processorCapabilityRepository.save(capability);
        return ProcessorCapabilityResponse.from(capability);
    }

    @Transactional
    public ProcessorCapabilityResponse updateCapability(Long userId, Long capabilityId, ProcessorCapabilityRequest request) {
        ProcessorProfile profile = processorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        ProcessorCapability capability = processorCapabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Capability not found with id: " + capabilityId));

        if (!capability.getProcessor().getId().equals(profile.getId())) {
            throw new ForbiddenException("You cannot modify another processor's capability");
        }

        if (request.minQuantity().compareTo(request.maxQuantity()) > 0) {
            throw new BadRequestException("Minimum quantity cannot exceed maximum quantity");
        }

        if (!capability.getCategory().getId().equals(request.categoryId())) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

            if (processorCapabilityRepository.existsByProcessorIdAndCategoryId(profile.getId(), category.getId())) {
                throw new ConflictException("Capability for category '" + category.getName() + "' already exists");
            }
            capability.setCategory(category);
        }

        capability.setMinQuantity(request.minQuantity());
        capability.setMaxQuantity(request.maxQuantity());
        capability.setPricePerUnit(request.pricePerUnit());
        if (request.isActive() != null) {
            capability.setActive(request.isActive());
        }

        capability = processorCapabilityRepository.save(capability);
        return ProcessorCapabilityResponse.from(capability);
    }

    @Transactional
    public void deleteCapability(Long userId, Long capabilityId) {
        ProcessorProfile profile = processorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        ProcessorCapability capability = processorCapabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Capability not found with id: " + capabilityId));

        if (!capability.getProcessor().getId().equals(profile.getId())) {
            throw new ForbiddenException("You cannot delete another processor's capability");
        }

        processorCapabilityRepository.delete(capability);
    }

    @Transactional
    public ProcessorCapabilityResponse toggleCapabilityActive(Long userId, Long capabilityId, boolean active) {
        ProcessorProfile profile = processorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Processor profile not found for current user"));

        ProcessorCapability capability = processorCapabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Capability not found with id: " + capabilityId));

        if (!capability.getProcessor().getId().equals(profile.getId())) {
            throw new ForbiddenException("You cannot update another processor's capability");
        }

        capability.setActive(active);
        capability = processorCapabilityRepository.save(capability);
        return ProcessorCapabilityResponse.from(capability);
    }

    @Transactional(readOnly = true)
    public List<ProcessorCapabilityResponse> getEligibleCapabilitiesByCategory(Long categoryId) {
        return processorCapabilityRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                .stream()
                .filter(c -> c.getProcessor().isVerified())
                .map(ProcessorCapabilityResponse::from)
                .toList();
    }
}
