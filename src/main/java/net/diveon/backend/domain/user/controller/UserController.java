package net.diveon.backend.domain.user.controller;

import net.diveon.backend.domain.user.dto.AuthLoginRequest;
import net.diveon.backend.domain.user.dto.AuthLoginResponse;
import net.diveon.backend.domain.user.dto.AuthSignUpRequest;
import net.diveon.backend.domain.user.dto.RefreshTokenRequest;
import net.diveon.backend.domain.user.service.UserService;
import net.diveon.backend.domain.user.service.UserSignUpService;
import net.diveon.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final UserSignUpService userSignUpService;

    public UserController(UserService userService, UserSignUpService userSignUpService) {
        this.userService = userService;
        this.userSignUpService = userSignUpService;
    }

    @GetMapping("/check-loginId")
    public ResponseEntity<ApiResponse<Boolean>> checkLoginId(@RequestParam String loginId) {
        boolean available = userSignUpService.isLoginIdAvailable(loginId);
        String message = available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@RequestParam String nickname) {
        boolean available = userSignUpService.isNicknameAvailable(nickname);
        String message = available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthLoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공하였습니다.", response));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@Valid @RequestBody AuthSignUpRequest signupRequest) {
        userSignUpService.signup(signupRequest);
        return ResponseEntity.status(201).body(ApiResponse.success(("회원가입에 성공했습니다."), null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String newAccessToken = userService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal String userId) {
        userService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다.", null));
    }
}
