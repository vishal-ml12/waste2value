package com.waste2value.processor;

import com.waste2value.category.Category;
import com.waste2value.category.CategoryRepository;
import com.waste2value.notification.NotificationService;
import com.waste2value.processor.dto.ProcessorCapabilityRequest;
import com.waste2value.processor.dto.ProcessorCapabilityResponse;
import com.waste2value.processor.dto.ProcessorProfileRequest;
import com.waste2value.processor.dto.ProcessorProfileResponse;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.ForbiddenException;
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
class ProcessorServiceTest {

    @Mock
    private ProcessorProfileRepository processorProfileRepository;

    @Mock
    private ProcessorCapabilityRepository processorCapabilityRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProcessorService processorService;

    private User processorUser;
    private User generatorUser;
    private Category activeCategory;

    @BeforeEach
    void setUp() {
        processorUser = new User();
        processorUser.setId(4L);
        processorUser.setRole(Role.PROCESSOR);
        processorUser.setFirstName("Circular");
        processorUser.setLastName("Processor");

        generatorUser = new User();
        generatorUser.setId(2L);
        generatorUser.setRole(Role.GENERATOR);

        activeCategory = new Category();
        activeCategory.setId(8L);
        activeCategory.setName("PET Bottles");
        activeCategory.setUnit("kg");
        activeCategory.setActive(true);
    }

    @Test
    @DisplayName("Should create unverified processor profile")
    void shouldCreateProcessorProfile() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(processorUser));
        when(processorProfileRepository.findByUserId(4L)).thenReturn(Optional.empty());
        when(processorProfileRepository.save(any(ProcessorProfile.class))).thenAnswer(i -> i.getArgument(0));

        ProcessorProfileRequest req = new ProcessorProfileRequest("Recycle Corp", "LIC-1234", "High-tech facility", new BigDecimal("10000.00"));
        ProcessorProfileResponse resp = processorService.createOrUpdateProfile(4L, req);

        assertThat(resp).isNotNull();
        assertThat(resp.companyName()).isEqualTo("Recycle Corp");
        assertThat(resp.isVerified()).isFalse();
    }

    @Test
    @DisplayName("Non-processor cannot create processor profile")
    void nonProcessorCannotCreateProfile() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(generatorUser));

        ProcessorProfileRequest req = new ProcessorProfileRequest("Corp", "LIC", "Desc", new BigDecimal("5000.00"));

        assertThatThrownBy(() -> processorService.createOrUpdateProfile(2L, req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only users with role PROCESSOR");
    }

    @Test
    @DisplayName("Capability minQuantity cannot exceed maxQuantity")
    void capabilityMinCannotExceedMax() {
        ProcessorProfile profile = new ProcessorProfile();
        profile.setId(1L);
        profile.setUser(processorUser);

        when(processorProfileRepository.findByUserId(4L)).thenReturn(Optional.of(profile));

        ProcessorCapabilityRequest req = new ProcessorCapabilityRequest(8L, new BigDecimal("500.00"), new BigDecimal("100.00"), new BigDecimal("20.00"), true);

        assertThatThrownBy(() -> processorService.addCapability(4L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Minimum quantity cannot exceed maximum quantity");
    }

    @Test
    @DisplayName("Duplicate capability for same processor and category throws ConflictException")
    void duplicateCapabilityThrowsConflict() {
        ProcessorProfile profile = new ProcessorProfile();
        profile.setId(1L);
        profile.setUser(processorUser);

        when(processorProfileRepository.findByUserId(4L)).thenReturn(Optional.of(profile));
        when(categoryRepository.findById(8L)).thenReturn(Optional.of(activeCategory));
        when(processorCapabilityRepository.existsByProcessorIdAndCategoryId(1L, 8L)).thenReturn(true);

        ProcessorCapabilityRequest req = new ProcessorCapabilityRequest(8L, new BigDecimal("50.00"), new BigDecimal("500.00"), new BigDecimal("20.00"), true);

        assertThatThrownBy(() -> processorService.addCapability(4L, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }
}
