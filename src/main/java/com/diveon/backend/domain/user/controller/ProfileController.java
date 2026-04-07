package com.diveon.backend.domain.user.controller;

import com.diveon.backend.domain.user.dto.ProfileShowResponse;
import com.diveon.backend.domain.user.service.ProfileService;
import com.diveon.backend.global.response.ApiResponse;
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

    @GetMapping("/show")
    public ResponseEntity<ApiResponse<ProfileShowResponse>> show(Authentication authentication) {
        String userId = authentication.getName();
        ProfileShowResponse response = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("성공", response));
    }
}
