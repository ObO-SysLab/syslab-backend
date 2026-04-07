package com.syslab.backend.domain.user.service;

import com.syslab.backend.domain.user.dto.AuthLoginRequest;
import com.syslab.backend.domain.user.dto.AuthLoginResponse;
import com.syslab.backend.domain.user.entity.User;
import com.syslab.backend.domain.user.repository.UserRepository;
import com.syslab.backend.global.exception.InvalidCredentialsException;
import com.syslab.backend.global.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        // 아이디로 유저 조회, 없으면 401
        User user = userRepository.findById(request.getUser_id())
                .orElseThrow(InvalidCredentialsException::new);

        // 비밀번호 검증 (BCrypt), 불일치 시 401
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // JWT access/refresh 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        // 응답 데이터 구성 (토큰 + 유저 기본 정보)
        AuthLoginResponse.UserInfo userInfo = new AuthLoginResponse.UserInfo(
                user.getNickName(),
                user.getProfileUrl()
        );

        return new AuthLoginResponse(accessToken, refreshToken, userInfo);
    }
}
