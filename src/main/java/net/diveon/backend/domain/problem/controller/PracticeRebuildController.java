package net.diveon.backend.domain.problem.controller;

import net.diveon.backend.domain.problem.dto.request.PracticeRebuildRequest;
import net.diveon.backend.domain.problem.service.PracticeRebuildService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class PracticeRebuildController {
    private final PracticeRebuildService rebuilds;

    public PracticeRebuildController(PracticeRebuildService rebuilds) {
        this.rebuilds = rebuilds;
    }

    // 실습 이미지 다시 만들기: 새 Dockerfile 또는 기존 소스 + 허용 명령어
    @PostMapping(value = "/practice/{probId}/rebuild", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<PracticeRebuildService.Result>> rebuild(
            @AuthenticationPrincipal String userId,
            @PathVariable("probId") long probId,
            @ModelAttribute PracticeRebuildRequest request) {
        var result = rebuilds.rebuild(Long.parseLong(userId), probId, request);
        return ResponseEntity.accepted().body(ApiResponse.success("실습 환경을 다시 만들고 있습니다.", result));
    }
}
