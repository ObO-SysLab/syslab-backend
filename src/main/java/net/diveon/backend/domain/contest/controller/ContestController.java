package net.diveon.backend.domain.contest.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.contest.dto.request.ContestCreateRequest;
import net.diveon.backend.domain.contest.dto.request.ContestUpdateRequest;
import net.diveon.backend.domain.contest.dto.response.ContestCreateResponse;
import net.diveon.backend.domain.contest.dto.response.ContestDetailResponse;
import net.diveon.backend.domain.contest.dto.response.ContestJoinResponse;
import net.diveon.backend.domain.contest.dto.response.ContestListResponse;
import net.diveon.backend.domain.contest.service.ContestService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    // 대회 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<ContestListResponse>> getContestList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onlyJoined,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal String userId) {
        Long parsedUserId = userId != null ? Long.parseLong(userId) : null;
        ContestListResponse response = contestService.getContestList(keyword, status, onlyJoined, page, size, parsedUserId);
        return ResponseEntity.ok(ApiResponse.success("대회 목록 조회 성공", response));
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

    // 대회 참가
    @PostMapping("/{contestId}/join")
    public ResponseEntity<ApiResponse<ContestJoinResponse>> joinContest(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId) {
        ContestJoinResponse response = contestService.joinContest(contestId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("대회 참가가 완료되었습니다.", response));
    }

    // 대회 참가 취소
    @DeleteMapping("/{contestId}/leave")
    public ResponseEntity<ApiResponse<ContestJoinResponse>> leaveContest(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId) {
        ContestJoinResponse response = contestService.leaveContest(contestId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("대회 참가 취소가 완료되었습니다.", response));
    }

    // 대회 설정 수정
    @PatchMapping("/{contestId}")
    public ResponseEntity<ApiResponse<Void>> updateContest(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ContestUpdateRequest request) {
        contestService.updateContest(contestId, Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("대회 설정이 저장되었습니다.", null));
    }

    // 대회 삭제
    @DeleteMapping("/{contestId}")
    public ResponseEntity<ApiResponse<Void>> deleteContest(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId) {
        contestService.deleteContest(contestId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("대회가 성공적으로 삭제되었습니다.", null));
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
