package net.diveon.backend.domain.group.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.group.dto.GroupCommentRequest;
import net.diveon.backend.domain.group.dto.GroupCommentResponse;
import net.diveon.backend.domain.group.dto.GroupPostRequest;
import net.diveon.backend.domain.group.dto.GroupPostResponse;
import net.diveon.backend.domain.group.service.GroupPostCommentService;
import net.diveon.backend.domain.group.service.GroupPostService;
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
@RequestMapping("/api/groups")
public class GroupPostController {

    private final GroupPostService groupPostService;
    private final GroupPostCommentService groupPostCommentService;

    public GroupPostController(GroupPostService groupPostService, GroupPostCommentService groupPostCommentService) {
        this.groupPostService = groupPostService;
        this.groupPostCommentService = groupPostCommentService;
    }

    // 게시글 목록 조회
    @GetMapping("/{groupId}/posts")
    public ResponseEntity<ApiResponse<GroupPostResponse.PostList>> getPostList(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        GroupPostResponse.PostList response = groupPostService.getPostList(groupId, Long.parseLong(userId), page, size);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록 조회 성공", response));
    }

    // 게시글 상세 조회
    @GetMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<ApiResponse<GroupPostResponse.PostDetailItem>> getPostDetail(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @AuthenticationPrincipal String userId) {
        GroupPostResponse.PostDetailItem response = groupPostService.getPostDetail(groupId, postId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("게시글 상세 조회 성공", response));
    }

    // 게시글 생성
    @PostMapping("/{groupId}/posts")
    public ResponseEntity<ApiResponse<GroupPostResponse.PostCreate>> createPost(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody GroupPostRequest.Create request) {
        GroupPostResponse.PostCreate response = groupPostService.createPost(groupId, Long.parseLong(userId), request);
        return ResponseEntity.status(201).body(ApiResponse.created("게시글이 성공적으로 등록되었습니다.", response));
    }

    // 게시글 수정
    @PatchMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @AuthenticationPrincipal String userId,
            @RequestBody GroupPostRequest.Update request) {
        groupPostService.updatePost(groupId, postId, Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("게시글이 성공적으로 수정되었습니다.", null));
    }

    // 게시글 삭제
    @DeleteMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @AuthenticationPrincipal String userId) {
        groupPostService.deletePost(groupId, postId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다.", null));
    }

    // 댓글 생성
    @PostMapping("/{groupId}/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<GroupCommentResponse.CommentCreate>> createComment(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody GroupCommentRequest.Create request) {
        GroupCommentResponse.CommentCreate response = groupPostCommentService.createComment(groupId, postId, Long.parseLong(userId), request);
        return ResponseEntity.status(201).body(ApiResponse.created("댓글이 등록되었습니다.", response));
    }

    // 댓글 수정
    @PatchMapping("/{groupId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal String userId,
            @RequestBody GroupCommentRequest.Update request) {
        groupPostCommentService.updateComment(groupId, postId, commentId, Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("댓글이 성공적으로 수정되었습니다.", null));
    }

    // 댓글 삭제
    @DeleteMapping("/{groupId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal String userId) {
        groupPostCommentService.deleteComment(groupId, postId, commentId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다.", null));
    }
}
