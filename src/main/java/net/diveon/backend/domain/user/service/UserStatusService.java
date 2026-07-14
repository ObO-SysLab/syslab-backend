package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStatusService {

    private final UserRepository userRepository;

    public UserStatusService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean isActiveUser(Long userId) {
        return userRepository.findById(userId)
                .map(user -> !user.isDeleted())
                .orElse(false);
    }
}
