package net.diveon.backend.domain.problem.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.problem.dto.request.ProblemCommentCreateRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemCommentUpdateRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCommentResponse;
import net.diveon.backend.domain.problem.service.ProblemCommentService;
import net.diveon.backend.global.response.ApiResponse;
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

@RestController
@RequestMapping("/api/problems")
public class ProblemCommentController {

    private final ProblemCommentService commentService;

    public ProblemCommentController(ProblemCommentService commentService) {
        this.commentService = commentService;
    }

    // 댓글 조회
    @GetMapping("/{probId}/comments")
    public ResponseEntity<ApiResponse<ProblemCommentResponse.CommentList>> getComments(
        @AuthenticationPrincipal String userId,
        @PathVariable("probId") long probId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        ProblemCommentResponse.CommentList responseData =
            commentService.getComments(Long.parseLong(userId), probId, page, size);

        return ResponseEntity.status(200)
            .body(ApiResponse.success("댓글 목록 조회에 성공하였습니다.", responseData));
    }

    // 댓글 상세 조회
    @GetMapping("/{probId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<ProblemCommentResponse.CommentItem>> getComment(
        @AuthenticationPrincipal String userId,
        @PathVariable("probId") long probId,
        @PathVariable("commentId") long commentId
    ) {
        ProblemCommentResponse.CommentItem responseData =
            commentService.getComment(Long.parseLong(userId), probId, commentId);

        return ResponseEntity.status(200)
            .body(ApiResponse.success("댓글 상세 조회에 성공하였습니다.", responseData));
    }

    // 댓글 생성
    @PostMapping("/{probId}/comments")
    public ResponseEntity<ApiResponse<ProblemCommentResponse.CommentCreate>> createComment(
        @AuthenticationPrincipal String userId,
        @PathVariable("probId") long probId,
        @Valid @RequestBody ProblemCommentCreateRequest request
    ) {
        ProblemCommentResponse.CommentCreate responseData =
            commentService.createComment(Long.parseLong(userId), probId, request);

        return ResponseEntity.status(201)
            .body(ApiResponse.success("댓글이 작성되었습니다.", responseData));
    }

    // 댓글 수정
    @PatchMapping("/{probId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<ProblemCommentResponse.CommentUpdate>> updateComment(
        @AuthenticationPrincipal String userId,
        @PathVariable("probId") long probId,
        @PathVariable("commentId") long commentId,
        @RequestBody ProblemCommentUpdateRequest request
    ) {
        ProblemCommentResponse.CommentUpdate responseData =
            commentService.updateComment(Long.parseLong(userId), probId, commentId, request);

        return ResponseEntity.status(200)
            .body(ApiResponse.success("댓글이 수정되었습니다.", responseData));
    }

    // 댓글 삭제
    @DeleteMapping("/{probId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<ProblemCommentResponse.CommentDelete>> deleteComment(
        @AuthenticationPrincipal String userId,
        @PathVariable("probId") long probId,
        @PathVariable("commentId") long commentId
    ) {
        ProblemCommentResponse.CommentDelete responseData =
            commentService.deleteComment(Long.parseLong(userId), probId, commentId);

        return ResponseEntity.status(200)
            .body(ApiResponse.success("댓글이 삭제되었습니다.", responseData));
    }
}
