package net.diveon.backend.domain.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import net.diveon.backend.domain.user.dto.AuthLoginResponse;
import net.diveon.backend.domain.user.dto.GoogleLoginRequest;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.GoogleOAuthException;
import net.diveon.backend.global.exception.InvalidCredentialsException;
import net.diveon.backend.global.exception.SocialAccountConflictException;
import net.diveon.backend.global.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Profile("prod")
public class GoogleOAuthService {

    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_LOGIN_ID_PREFIX = "google:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String REQUESTED_WITH_HEADER_VALUE = "XmlHttpRequest";
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;
    private static final int NICKNAME_MAX_LENGTH = 50;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final RestClient restClient;
    private final GoogleIdTokenVerifier idTokenVerifier;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final Set<String> allowedOrigins;

    public GoogleOAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            RedisTemplate<String, String> redisTemplate,
            RestClient.Builder restClientBuilder,
            @Value("${auth.google.client-id}") String clientId,
            @Value("${auth.google.client-secret}") String clientSecret,
            @Value("${auth.google.redirect-uri}") String redirectUri,
            @Value("${auth.google.allowed-origins:}") String allowedOrigins) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
        this.idTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Transactional
    public AuthLoginResponse login(GoogleLoginRequest request, String requestedWith, String origin) {
        validateCodeRequest(requestedWith, origin);

        GoogleTokenResponse tokenResponse = exchangeCode(request.getCode());
        GoogleIdToken.Payload payload = verifyIdToken(tokenResponse.getIdToken());

        String googleSubject = payload.getSubject();
        String loginId = GOOGLE_LOGIN_ID_PREFIX + googleSubject;

        User user = userRepository.findByLoginId(loginId)
                .orElseGet(() -> createUser(loginId, payload));

        if (user.isDeleted()) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    private void validateCodeRequest(String requestedWith, String origin) {
        if (!REQUESTED_WITH_HEADER_VALUE.equals(requestedWith)) {
            throw new GoogleOAuthException("유효하지 않은 구글 로그인 요청입니다.");
        }

        if (!allowedOrigins.isEmpty() && (origin == null || !allowedOrigins.contains(origin))) {
            throw new GoogleOAuthException("허용되지 않은 출처의 구글 로그인 요청입니다.");
        }
    }

    private GoogleTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse tokenResponse = restClient.post()
                    .uri(GOOGLE_TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

            if (tokenResponse == null || tokenResponse.getIdToken() == null || tokenResponse.getIdToken().isBlank()) {
                throw new GoogleOAuthException("구글 토큰 응답에 ID 토큰이 없습니다.");
            }
            return tokenResponse;
        } catch (RestClientException ex) {
            throw new GoogleOAuthException("구글 토큰 교환에 실패했습니다.");
        }
    }

    private GoogleIdToken.Payload verifyIdToken(String idToken) {
        try {
            GoogleIdToken verifiedIdToken = idTokenVerifier.verify(idToken);
            if (verifiedIdToken == null) {
                throw new GoogleOAuthException("유효하지 않은 구글 ID 토큰입니다.");
            }

            GoogleIdToken.Payload payload = verifiedIdToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new GoogleOAuthException("구글 이메일 인증이 완료되지 않은 계정입니다.");
            }
            return payload;
        } catch (GeneralSecurityException | IOException ex) {
            throw new GoogleOAuthException("구글 ID 토큰 검증에 실패했습니다.");
        }
    }

    private User createUser(String loginId, GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        if (email == null || email.isBlank()) {
            throw new GoogleOAuthException("구글 계정 이메일을 확인할 수 없습니다.");
        }

        Optional<User> existingEmailUser = userRepository.findByEmail(email);
        if (existingEmailUser.isPresent()) {
            throw new SocialAccountConflictException();
        }

        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        String encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        String nickname = createUniqueNickname(name, email);

        User user = User.createSocialUser(loginId, encodedPassword, nickname, email, name, picture);
        return userRepository.save(user);
    }

    private String createUniqueNickname(String name, String email) {
        String base = name;
        if (base == null || base.isBlank()) {
            base = email.substring(0, email.indexOf("@"));
        }

        base = base.replaceAll("[^가-힣a-zA-Z0-9_-]", "");
        if (base.isBlank()) {
            base = "google_user";
        }
        base = truncate(base, NICKNAME_MAX_LENGTH);

        if (!userRepository.existsByNickname(base)) {
            return base;
        }

        for (int i = 0; i < 10; i++) {
            String suffix = "_" + UUID.randomUUID().toString().substring(0, 8);
            String candidate = truncate(base, NICKNAME_MAX_LENGTH - suffix.length()) + suffix;
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }

        throw new GoogleOAuthException("사용 가능한 닉네임 생성에 실패했습니다.");
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private AuthLoginResponse issueTokens(User user) {
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

    private Set<String> parseAllowedOrigins(String allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return Collections.emptySet();
        }

        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static class GoogleTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private Long expiresIn;
        @JsonProperty("id_token")
        private String idToken;
        private String scope;
        @JsonProperty("token_type")
        private String tokenType;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getIdToken() {
            return idToken;
        }

        public void setIdToken(String idToken) {
            this.idToken = idToken;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }
    }
}
