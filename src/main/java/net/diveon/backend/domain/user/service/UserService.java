package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.user.dto.AuthLoginRequest;
import net.diveon.backend.domain.user.dto.AuthLoginResponse;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.InvalidCredentialsException;
import net.diveon.backend.global.exception.InvalidTokenException;
import net.diveon.backend.global.security.JwtProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider, RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtProvider.generateAccessToken(String.valueOf(user.getId()));
        String refreshToken = jwtProvider.generateRefreshToken(String.valueOf(user.getId()));

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                REFRESH_TOKEN_TTL_DAYS,
                TimeUnit.DAYS
        );

        AuthLoginResponse.UserInfo userInfo = new AuthLoginResponse.UserInfo(
                user.getNickname(),
                user.getProfileImgUrl()
        );

        return new AuthLoginResponse(accessToken, refreshToken, userInfo);
    }

    public String refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException();
        }
        String userId = jwtProvider.getUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
        if (!refreshToken.equals(stored)) {
            throw new InvalidTokenException();
        }
        return jwtProvider.generateAccessToken(userId);
    }

    public void logout(String userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}
