package net.diveon.backend.domain.contest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.contest.dto.request.ContestParticipantBanRequest;
import net.diveon.backend.domain.contest.dto.response.ContestParticipantBanResponse;
import net.diveon.backend.domain.contest.dto.response.ContestParticipantListResponse;
import net.diveon.backend.domain.contest.service.ContestParticipantService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/contests/{contestId}/participants")
public class ContestParticipantController {

    private final ContestParticipantService contestParticipantService;

    public ContestParticipantController(ContestParticipantService contestParticipantService) {
        this.contestParticipantService = contestParticipantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContestParticipantListResponse>> getContestParticipants(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        ContestParticipantListResponse response = contestParticipantService.getContestParticipants(
                contestId, Long.parseLong(userId), keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("참가자 목록 조회 성공", response));
    }

    @GetMapping("/banned")
    public ResponseEntity<ApiResponse<ContestParticipantListResponse>> getBannedContestParticipants(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        ContestParticipantListResponse response = contestParticipantService.getBannedContestParticipants(
                contestId, Long.parseLong(userId), keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("차단된 참가자 목록 조회 성공", response));
    }

    @PatchMapping("/{userId}/ban")
    public ResponseEntity<ApiResponse<ContestParticipantBanResponse>> updateContestParticipantBanStatus(
            @PathVariable Long contestId,
            @PathVariable("userId") Long participantUserId,
            @AuthenticationPrincipal String userId,
            @RequestBody ContestParticipantBanRequest request) {
        ContestParticipantBanResponse response = contestParticipantService.updateContestParticipantBanStatus(
                contestId, participantUserId, Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("해당 참가자의 상태가 변경되었습니다.", response));
    }
}
