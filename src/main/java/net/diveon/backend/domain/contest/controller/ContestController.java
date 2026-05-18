package net.diveon.backend.domain.contest.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.contest.dto.request.ContestCreateRequest;
import net.diveon.backend.domain.contest.dto.response.ContestCreateResponse;
import net.diveon.backend.domain.contest.dto.response.ContestDetailResponse;
import net.diveon.backend.domain.contest.service.ContestService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    // 대회 상세 조회
    @GetMapping("/{contestId}")
    public ResponseEntity<ApiResponse<ContestDetailResponse>> getContestDetail(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId) {
        Long parsedUserId = userId != null ? Long.parseLong(userId) : null;
        ContestDetailResponse response = contestService.getContestDetail(contestId, parsedUserId);
        return ResponseEntity.ok(ApiResponse.success("대회 상세 정보 조회 성공", response));
    }

    // 대회 생성
    @PostMapping
    public ResponseEntity<ApiResponse<ContestCreateResponse>> createContest(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ContestCreateRequest request) {
        ContestCreateResponse response = contestService.createContest(Long.parseLong(userId), request);
        return ResponseEntity.status(201).body(ApiResponse.created("대회가 성공적으로 생성되었습니다.", response));
    }
}
