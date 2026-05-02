package net.diveon.backend.domain.grade.controller;

import net.diveon.backend.domain.grade.dto.response.GradeBoardObjectiveResponse;
import net.diveon.backend.domain.grade.service.GradeBoardService;
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
public class GradeBoardController {

    private final GradeBoardService gradeBoardService;

    public GradeBoardController(GradeBoardService gradeBoardService) {
        this.gradeBoardService = gradeBoardService;
    }

    @GetMapping("/{prob_id}/board/multiple-choice")
    public ResponseEntity<ApiResponse<GradeBoardObjectiveResponse>> getObjectiveBoard(
            @AuthenticationPrincipal String userId,
            @PathVariable("prob_id") Long probId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String result,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        GradeBoardObjectiveResponse response = gradeBoardService.getObjectiveBoard(probId, page, size, result, sort);
        return ResponseEntity.ok(ApiResponse.success("객관식 채점 보드 조회에 성공하였습니다.", response));
    }
}
