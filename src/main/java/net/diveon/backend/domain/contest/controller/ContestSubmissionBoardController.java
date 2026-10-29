package net.diveon.backend.domain.contest.controller;

import net.diveon.backend.domain.contest.dto.response.ContestSubmissionBoardResponse;
import net.diveon.backend.domain.contest.service.ContestSubmissionBoardService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contests/{contestId}/problems/{contestProblemId}/board")
public class ContestSubmissionBoardController {
    private final ContestSubmissionBoardService service;
    public ContestSubmissionBoardController(ContestSubmissionBoardService service) { this.service = service; }
    @GetMapping
    public ResponseEntity<ApiResponse<ContestSubmissionBoardResponse>> getBoard(@AuthenticationPrincipal String userId,
            @PathVariable Long contestId, @PathVariable Long contestProblemId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(ApiResponse.success("대회 제출 기록을 조회했습니다.", service.getBoard(Long.parseLong(userId), contestId, contestProblemId, page, size)));
    }
}
