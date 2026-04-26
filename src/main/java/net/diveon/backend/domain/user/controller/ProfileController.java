package net.diveon.backend.domain.user.controller;

import net.diveon.backend.domain.user.dto.ProfileShowResponse;
import net.diveon.backend.domain.user.service.ProfileService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/show")
    public ResponseEntity<ApiResponse<ProfileShowResponse>> show(@AuthenticationPrincipal String userId) {
        ProfileShowResponse response = profileService.getProfile(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("성공", response));
    }
}
