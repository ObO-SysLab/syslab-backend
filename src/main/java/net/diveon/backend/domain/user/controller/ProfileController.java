package net.diveon.backend.domain.user.controller;

import net.diveon.backend.domain.user.dto.ProfileShowResponse;
import net.diveon.backend.domain.user.service.ProfileService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // GET /api/profile/show
    // JwtFilter가 SecurityContext에 저장한 인증 정보에서 userId 추출
    // 별도로 토큰 파싱 없이 Authentication 객체로 userId 바로 사용 가능
    @GetMapping("/show")
    public ResponseEntity<ApiResponse<ProfileShowResponse>> show(Authentication authentication) {
        String userId = authentication.getName(); // userId 꺼냄
        ProfileShowResponse response = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("성공", response));
    }
}
