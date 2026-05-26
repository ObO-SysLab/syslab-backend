package net.diveon.backend.domain.user.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.user.dto.PasswordUpdateRequest;
import net.diveon.backend.domain.user.dto.ProfileShowResponse;
import net.diveon.backend.domain.user.dto.ProfileUpdateRequest;
import net.diveon.backend.domain.user.service.ProfileService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }
    // 프로필 조회
    @GetMapping("/show")
    public ResponseEntity<ApiResponse<ProfileShowResponse>> show(@AuthenticationPrincipal String userId) {
        ProfileShowResponse response = profileService.getProfile(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("성공", response));
    }

    // 프로필 수정
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody ProfileUpdateRequest request) {
        profileService.updateProfile(Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", null));
    }

    // 비밀번호 변경

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PasswordUpdateRequest request) {
        profileService.updatePassword(Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }
}
