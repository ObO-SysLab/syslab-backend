package net.diveon.backend.domain.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import net.diveon.backend.domain.user.dto.AuthLoginResponse;
import net.diveon.backend.domain.user.dto.GoogleLoginRequest;
import net.diveon.backend.domain.user.service.GoogleOAuthService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("prod")
@RestController
@RequestMapping("/api/auth/google")
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    public GoogleOAuthController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest servletRequest) {
        AuthLoginResponse response = googleOAuthService.login(
                request,
                servletRequest.getHeader("X-Requested-With"),
                servletRequest.getHeader("Origin")
        );
        return ResponseEntity.ok(ApiResponse.success("구글 로그인에 성공하였습니다.", response));
    }
}
