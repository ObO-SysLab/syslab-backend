package com.diveon.backend.domain.user.service;

import com.diveon.backend.domain.user.dto.ProfileShowResponse;
import com.diveon.backend.domain.user.entity.User;
import com.diveon.backend.domain.user.repository.UserRepository;
import com.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ProfileShowResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return new ProfileShowResponse(new ProfileShowResponse.UserInfo(user));
    }
}
