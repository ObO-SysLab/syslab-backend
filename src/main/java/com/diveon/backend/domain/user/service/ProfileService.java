package com.diveon.backend.domain.user.service;

import com.diveon.backend.domain.user.dto.ProfileShowResponse;
import com.diveon.backend.domain.user.entity.User;
import com.diveon.backend.domain.user.repository.UserRepository;
import com.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

// 프로필 수정 API 구현할 때 이 파일에 메서드 추가할 것임
@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // userId로 유저 조회 후 ProfileShowResponse로 변환하여 반환
    // 유저가 없으면 UserNotFoundException → 404
    public ProfileShowResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return new ProfileShowResponse(new ProfileShowResponse.UserInfo(user));
    }
}
