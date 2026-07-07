package net.diveon.backend.domain.user.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.user.dto.AuthLoginResponse;
import net.diveon.backend.domain.user.dto.GoogleLoginRequest;
import net.diveon.backend.domain.user.service.LocalGoogleOAuthService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/api/local/auth/google")
public class LocalGoogleOAuthController {

    private final LocalGoogleOAuthService localGoogleOAuthService;

    public LocalGoogleOAuthController(LocalGoogleOAuthService localGoogleOAuthService) {
        this.localGoogleOAuthService = localGoogleOAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthLoginResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthLoginResponse response = localGoogleOAuthService.login(request);
        return ResponseEntity.ok(ApiResponse.success("구글 로그인에 성공하였습니다.", response));
    }
}
