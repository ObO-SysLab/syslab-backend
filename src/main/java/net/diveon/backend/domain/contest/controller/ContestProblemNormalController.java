package net.diveon.backend.domain.contest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.contest.dto.request.ContestProblemPointsUpdateRequest;
import net.diveon.backend.domain.contest.dto.response.ContestProblemListResponse;
import net.diveon.backend.domain.contest.service.ContestProblemNormalService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;


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

    @PostMapping("/{contestId}/problems/{problemId}/add/{point}")
    public ResponseEntity<ApiResponse<Void>> addProblemToContest(
        @PathVariable Long contestId,
        @PathVariable Long problemId,
        @PathVariable Integer point,
        @AuthenticationPrincipal String userId
    ){
        contestProblemNormalService.addProblemToContestServiceLogic(
            contestId, 
            problemId, 
            Long.parseLong(userId), 
            point);
        return ResponseEntity.ok(ApiResponse.success("해당 문제가 대회에 추가되었습니다.", null));
    }
    
}
