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
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.contest.dto.request.QnaAnswerRequest;
import net.diveon.backend.domain.contest.dto.request.QnaCreateRequest;
import net.diveon.backend.domain.contest.dto.response.QnaListResponse;
import net.diveon.backend.domain.contest.service.ContestQnaService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/contests/{contestId}/qnas")
public class ContestQnaController {

    private final ContestQnaService contestQnaService;

    public ContestQnaController(ContestQnaService contestQnaService) {
        this.contestQnaService = contestQnaService;
    }

    // 8-1. Q&A 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<QnaListResponse>> getQnaList(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId) {
        QnaListResponse response = contestQnaService.getQnaList(contestId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("Q&A 목록 조회 성공", response));
    }

    // 8-2. 질문 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createQuestion(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody QnaCreateRequest request) {
        contestQnaService.createQuestion(contestId, Long.parseLong(userId), request);
        return ResponseEntity.status(201).body(ApiResponse.created("질문이 등록되었습니다.", null));
    }

    // 8-3. 질문 삭제
    @DeleteMapping("/{qnaId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @PathVariable Long contestId,
            @PathVariable Long qnaId,
            @AuthenticationPrincipal String userId) {
        contestQnaService.deleteQuestion(contestId, qnaId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("질문이 삭제되었습니다.", null));
    }

    // 8-4. 답변 생성
    @PatchMapping("/{qnaId}/answer")
    public ResponseEntity<ApiResponse<Void>> createAnswer(
            @PathVariable Long contestId,
            @PathVariable Long qnaId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody QnaAnswerRequest request) {
        contestQnaService.createAnswer(contestId, qnaId, Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("답변이 등록되었습니다.", null));
    }

    // 8-5. 답변 삭제
    @DeleteMapping("/{qnaId}/answer")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(
            @PathVariable Long contestId,
            @PathVariable Long qnaId,
            @AuthenticationPrincipal String userId) {
        contestQnaService.deleteAnswer(contestId, qnaId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("답변이 삭제되었습니다.", null));
    }
}
