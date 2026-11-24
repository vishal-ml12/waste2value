package com.waste2value.address;

import com.waste2value.address.dto.AddressRequest;
import com.waste2value.address.dto.AddressResponse;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.User;
import com.waste2value.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserService userService;

    public AddressService(AddressRepository addressRepository, UserService userService) {
        this.addressRepository = addressRepository;
        this.userService = userService;
    }

    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userService.getUserEntity(userId);
        long existingCount = addressRepository.countByUserId(userId);

        boolean makeDefault = (existingCount == 0) || Boolean.TRUE.equals(request.isDefault());

        if (makeDefault) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = new Address(
                user,
                request.addressLine1().trim(),
                request.addressLine2() != null ? request.addressLine2().trim() : null,
                request.city().trim(),
                request.state().trim(),
                request.postalCode().trim(),
                request.latitude(),
                request.longitude(),
                makeDefault
        );

        Address saved = addressRepository.save(address);
        return AddressResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long id, Long userId) {
        Address address = getAddressEntity(id, userId);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse updateAddress(Long id, Long userId, AddressRequest request) {
        Address address = getAddressEntity(id, userId);

        if (Boolean.TRUE.equals(request.isDefault()) && !address.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(request.isDefault()) && address.isDefault()) {
            // Cannot unset the only default address without setting another
            long count = addressRepository.countByUserId(userId);
            if (count > 1) {
                address.setDefault(false);
            }
        }

        address.setAddressLine1(request.addressLine1().trim());
        address.setAddressLine2(request.addressLine2() != null ? request.addressLine2().trim() : null);
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());

        Address updated = addressRepository.save(address);
        return AddressResponse.from(updated);
    }

    @Transactional
    public void deleteAddress(Long id, Long userId) {
        Address address = getAddressEntity(id, userId);
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        // If the deleted address was default, make the most recent remaining address default
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long id, Long userId) {
        Address address = getAddressEntity(id, userId);
        if (!address.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
            address.setDefault(true);
            address = addressRepository.save(address);
        }
        return AddressResponse.from(address);
    }

    @Transactional(readOnly = true)
    public Address getAddressEntity(Long id, Long userId) {
        // Enforce ownership: Return generic NOT FOUND to prevent leaking existence across users
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
