package net.diveon.backend.domain.user.controller;

import net.diveon.backend.domain.user.dto.ProfileImageUploadResponse;
import net.diveon.backend.domain.user.service.ProfileService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileImageController {

    private final ProfileService profileService;

    public UserProfileImageController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // 프로필 이미지 업로드
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileImageUploadResponse>> uploadProfileImage(
            @AuthenticationPrincipal String userId,
            @RequestParam("image") MultipartFile image) {
        String imageUrl = profileService.uploadProfileImage(Long.parseLong(userId), image);
        return ResponseEntity.ok(ApiResponse.success("프로필 이미지 업로드 성공", new ProfileImageUploadResponse(imageUrl)));
    }
}
