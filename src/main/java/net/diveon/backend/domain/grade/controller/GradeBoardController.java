package net.diveon.backend.domain.grade.controller;

import net.diveon.backend.domain.grade.dto.response.interfaces.GradeBoardResponse;
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

    @GetMapping("/{probId}/board")
    public ResponseEntity<ApiResponse<GradeBoardResponse>> getBoard(
            @AuthenticationPrincipal String userId,
            @PathVariable("probId") Long probId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String result,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        GradeBoardResponse response = gradeBoardService.getBoard(probId, page, size, result, sort);
        return ResponseEntity.ok(ApiResponse.success("채점 보드 조회에 성공하였습니다.", response));
    }
}