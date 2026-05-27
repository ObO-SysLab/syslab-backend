package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.user.dto.PasswordUpdateRequest;
import net.diveon.backend.domain.user.dto.ProfileShowResponse;
import net.diveon.backend.domain.user.dto.ProfileUpdateRequest;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.InvalidCredentialsException;
import net.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 프로필 수정 API 구현할 때 이 파일에 메서드 추가할 것임
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileShowResponse getProfile(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return new ProfileShowResponse(new ProfileShowResponse.UserInfo(user));
    }

    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.updateProfile(
                request.getNickname(),
                request.getSelfComment(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getBelong(),
                request.getInterest()
        );
    }

    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }
}
