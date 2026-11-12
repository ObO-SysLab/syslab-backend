package net.diveon.backend.domain.contest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.contest.dto.request.ContestProblemAddRequest;
import net.diveon.backend.domain.contest.dto.request.ContestProblemPointsUpdateRequest;
import net.diveon.backend.domain.contest.dto.response.ContestProblemListResponse;
import net.diveon.backend.domain.contest.service.ContestProblemNormalService;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import net.diveon.backend.global.response.ApiResponse;


@RestController
@RequestMapping("/api/contests")
public class ContestProblemNormalController {

    private final ContestProblemNormalService contestProblemNormalService;

    public ContestProblemNormalController(ContestProblemNormalService contestProblemNormalService) {
        this.contestProblemNormalService = contestProblemNormalService;
    }

    @GetMapping("/{contestId}/problems")
    public ResponseEntity<ApiResponse<ContestProblemListResponse>> getContestProblems(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId) {
        ContestProblemListResponse response = contestProblemNormalService.getContestProblems(
                contestId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("문제 목록 조회 성공", response));
    }

    // 주최자가 직접 출제한 기존 문제를 대회에 추가한다(검사는 서비스에서: 주최자·관리자·출제자 본인).
    @PostMapping("/{contestId}/problems")
    public ResponseEntity<ApiResponse<Void>> addContestProblem(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId,
            @RequestBody ContestProblemAddRequest request) {
        if (request == null || request.getProblemId() == null) {
            throw new ProblemNotFoundException("추가할 문제를 선택하세요.");
        }
        contestProblemNormalService.addProblemToContestServiceLogic(
                contestId, request.getProblemId(), Long.parseLong(userId), request.getPoints());
        return ResponseEntity.ok(ApiResponse.success("문제가 대회에 추가되었습니다.", null));
    }

    @DeleteMapping("/{contestId}/problems/{problemId}")
    public ResponseEntity<ApiResponse<Void>> deleteContestProblem(
            @PathVariable Long contestId,
            @PathVariable Long problemId,
            @AuthenticationPrincipal String userId) {
        contestProblemNormalService.deleteContestProblem(contestId, problemId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("문제가 대회에서 제거되었습니다.", null));
    }
    
    @PatchMapping("/{contestId}/problems/{problemId}/points")
    public ResponseEntity<ApiResponse<Void>> updateContestProblemPoints(
            @PathVariable Long contestId,
            @PathVariable Long problemId,
            @AuthenticationPrincipal String userId,
            @RequestBody ContestProblemPointsUpdateRequest request) {
        contestProblemNormalService.updateContestProblemPoints(
                contestId, problemId, Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("문제 배점이 수정되었습니다.", null));
    }

    
}
