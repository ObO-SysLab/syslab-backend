package net.diveon.backend.domain.problem.controller;

import net.diveon.backend.domain.problem.dto.response.interfaces.ProblemDetailResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemListResponse;
import net.diveon.backend.domain.problem.service.ProblemDetailService;
import net.diveon.backend.domain.problem.service.ProblemListService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemDetailService problemDetailService;
    private final ProblemListService problemListService;

    public ProblemController(ProblemDetailService problemDetailService, ProblemListService problemListService) {
        this.problemDetailService = problemDetailService;
        this.problemListService = problemListService;
    }

    @GetMapping("")
    public ResponseEntity<ApiResponse<ProblemListResponse>> listProblems(
        @AuthenticationPrincipal String userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String difficulty,
        @RequestParam(defaultValue = "false") boolean onlyUnsolved,
        @RequestParam(defaultValue = "false") boolean onlyMine
    ) {
        ProblemListResponse responseData = problemListService.listProblems(
            Long.parseLong(userId),
            page,
            title,
            type,
            category,
            difficulty,
            onlyUnsolved,
            onlyMine
        );

        return ResponseEntity.status(200)
            .body(ApiResponse.success("문제 목록 조회에 성공하였습니다.", responseData));
    }

    /*
     * 현재 컨트롤러 반환 타입은 ProblemDetailResponse 공통 인터페이스를 사용합니다.
     * 추후 practice, coding 상세 조회 DTO가 추가되면 해당 DTO들도 ProblemDetailResponse를 구현하여
     * 같은 endpoint에서 문제 유형별 상세 응답을 반환할 수 있습니다.
     */
    @GetMapping("/{probId}")
    public ResponseEntity<ApiResponse<ProblemDetailResponse>> detailProblem(
        @AuthenticationPrincipal String userId,
        @PathVariable("probId") long probId
    ) {
        ProblemDetailResponse responseData =
            problemDetailService.detailProblem(Long.parseLong(userId), probId);

        return ResponseEntity.status(200)
            .body(ApiResponse.success("문제 상세 조회에 성공하였습니다.", responseData));
    }
}
