package com.waste2value.collector;

import com.waste2value.collector.dto.CollectorProfileRequest;
import com.waste2value.collector.dto.CollectorProfileResponse;
import com.waste2value.notification.NotificationService;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.user.AccountStatus;
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
class CollectorProfileServiceTest {

    @Mock
    private CollectorProfileRepository collectorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CollectorProfileService collectorProfileService;

    private User collectorUser;
    private User generatorUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        collectorUser = new User();
        collectorUser.setId(3L);
        collectorUser.setFirstName("City");
        collectorUser.setLastName("Collector");
        collectorUser.setEmail("collector@waste2value.com");
        collectorUser.setRole(Role.COLLECTOR);
        collectorUser.setStatus(AccountStatus.ACTIVE);

        generatorUser = new User();
        generatorUser.setId(2L);
        generatorUser.setFirstName("Eco");
        generatorUser.setLastName("Generator");
        generatorUser.setEmail("generator@waste2value.com");
        generatorUser.setRole(Role.GENERATOR);
        generatorUser.setStatus(AccountStatus.ACTIVE);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);
    }

    @Test
    @DisplayName("Should create collector profile unverified by default")
    void shouldCreateCollectorProfileUnverified() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(collectorUser));
        when(collectorProfileRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(collectorProfileRepository.save(any(CollectorProfile.class))).thenAnswer(invocation -> {
            CollectorProfile p = invocation.getArgument(0);
            return p;
        });

        CollectorProfileRequest request = new CollectorProfileRequest("Electric Van", "South Mumbai", new BigDecimal("45.00"));
        CollectorProfileResponse response = collectorProfileService.createOrUpdateProfile(3L, request);

        assertThat(response).isNotNull();
        assertThat(response.vehicleType()).isEqualTo("Electric Van");
        assertThat(response.serviceArea()).isEqualTo("South Mumbai");
        assertThat(response.isVerified()).isFalse();
    }

    @Test
    @DisplayName("Non-collector role cannot create collector profile")
    void nonCollectorRoleCannotCreateCollectorProfile() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(generatorUser));

        CollectorProfileRequest request = new CollectorProfileRequest("Van", "Area", new BigDecimal("30.00"));

        assertThatThrownBy(() -> collectorProfileService.createOrUpdateProfile(2L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only users with role COLLECTOR");
    }

    @Test
    @DisplayName("Admin can verify collector profile and trigger notification")
    void adminCanVerifyCollectorProfile() {
        CollectorProfile profile = new CollectorProfile();
        profile.setUser(collectorUser);
        profile.setVerified(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(collectorProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(collectorProfileRepository.save(any(CollectorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CollectorProfileResponse response = collectorProfileService.verifyCollector(1L, 10L, true, "Approved");

        assertThat(response.isVerified()).isTrue();
        verify(notificationService, times(1)).sendNotification(eq(collectorUser), any(), anyString(), anyString(), anyString(), any());
    }
}
