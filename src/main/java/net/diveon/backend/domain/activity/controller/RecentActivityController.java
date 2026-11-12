package net.diveon.backend.domain.activity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.activity.dto.RecentActivityResponse;
import net.diveon.backend.domain.activity.service.RecentActivityService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/activity")
public class RecentActivityController {

    private final RecentActivityService recentActivityService;

    public RecentActivityController(RecentActivityService recentActivityService) {
        this.recentActivityService = recentActivityService;
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<RecentActivityResponse>> getRecent(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.success("최근 채점 결과 조회 성공", recentActivityService.getRecent(limit)));
    }
}
