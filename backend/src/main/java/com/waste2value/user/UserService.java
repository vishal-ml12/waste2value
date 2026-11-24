package com.waste2value.user;

import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.dto.UpdateProfileRequest;
import com.waste2value.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = getUserEntity(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserEntity(userId);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }
        User saved = userRepository.save(user);
        return UserProfileResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserProfileResponse::from)
                .toList();
    }
}
