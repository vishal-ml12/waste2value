package com.waste2value.address;

import com.waste2value.address.dto.AddressRequest;
import com.waste2value.address.dto.AddressResponse;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserService userService;

    private AddressService addressService;
    private User testUser;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository, userService);
        testUser = new User("Jane", "Doe", "jane@example.com", "hash", "1234567890", Role.GENERATOR);
        testUser.setId(1L);
    }

    @Test
    void createAddress_FirstAddress_AutomaticallyDefaultsToTrue() {
        AddressRequest request = new AddressRequest("123 Green Way", null, "Mumbai", "Maharashtra", "400001",
                new BigDecimal("19.0760"), new BigDecimal("72.8777"), false);

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(addressRepository.countByUserId(1L)).thenReturn(0L);

        Address saved = new Address(testUser, request.addressLine1(), null, request.city(), request.state(),
                request.postalCode(), request.latitude(), request.longitude(), true);
        saved.setId(100L);
        when(addressRepository.save(any(Address.class))).thenReturn(saved);

        AddressResponse response = addressService.createAddress(1L, request);

        assertNotNull(response);
        assertTrue(response.isDefault(), "First address must automatically be marked default");
        verify(addressRepository).clearDefaultForUser(1L);
    }

    @Test
    void createAddress_SecondAddressAsDefault_ClearsPriorDefaults() {
        AddressRequest request = new AddressRequest("456 Eco Park", null, "Pune", "Maharashtra", "411001",
                new BigDecimal("18.5204"), new BigDecimal("73.8567"), true);

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(addressRepository.countByUserId(1L)).thenReturn(1L);

        Address saved = new Address(testUser, request.addressLine1(), null, request.city(), request.state(),
                request.postalCode(), request.latitude(), request.longitude(), true);
        saved.setId(101L);
        when(addressRepository.save(any(Address.class))).thenReturn(saved);

        AddressResponse response = addressService.createAddress(1L, request);

        assertTrue(response.isDefault());
        verify(addressRepository).clearDefaultForUser(1L);
    }

    @Test
    void getAddressById_BelongingToAnotherUser_ThrowsResourceNotFoundException() {
        // Cross-user access check: Address exists but belongs to user 2
        when(addressRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.getAddressById(999L, 1L));
    }

    @Test
    void deleteAddress_WhenDefaultDeleted_PromotesRemainingAddress() {
        Address addressToDelete = new Address(testUser, "Old Default", null, "Mumbai", "MH", "400001",
                new BigDecimal("19.0"), new BigDecimal("72.0"), true);
        addressToDelete.setId(10L);

        when(addressRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(addressToDelete));

        Address remainingAddress = new Address(testUser, "Remaining", null, "Pune", "MH", "411001",
                new BigDecimal("18.0"), new BigDecimal("73.0"), false);
        remainingAddress.setId(20L);

        when(addressRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(remainingAddress));

        addressService.deleteAddress(10L, 1L);

        verify(addressRepository).delete(addressToDelete);
        assertTrue(remainingAddress.isDefault(), "Remaining address should be promoted to default");
        verify(addressRepository).save(remainingAddress);
    }

    @Test
    void setDefaultAddress_Success() {
        Address address = new Address(testUser, "Site 1", null, "Mumbai", "MH", "400001",
                new BigDecimal("19.0"), new BigDecimal("72.0"), false);
        address.setId(5L);

        when(addressRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.setDefaultAddress(5L, 1L);

        assertTrue(response.isDefault());
        verify(addressRepository).clearDefaultForUser(1L);
    }
}
