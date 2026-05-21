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

import net.diveon.backend.domain.contest.dto.request.NoticeCreateRequest;
import net.diveon.backend.domain.contest.dto.request.NoticeUpdateRequest;
import net.diveon.backend.domain.contest.dto.response.NoticeListResponse;
import net.diveon.backend.domain.contest.service.ContestNoticeService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/contests/{contestId}/notices")
public class ContestNoticeController {

    private final ContestNoticeService contestNoticeService;

    public ContestNoticeController(ContestNoticeService contestNoticeService) {
        this.contestNoticeService = contestNoticeService;
    }

    // 9-1. 공지 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<NoticeListResponse>> getNoticeList(
            @PathVariable Long contestId) {
        NoticeListResponse response = contestNoticeService.getNoticeList(contestId);
        return ResponseEntity.ok(ApiResponse.success("공지사항 목록 조회 성공", response));
    }

    // 9-2. 공지 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createNotice(
            @PathVariable Long contestId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody NoticeCreateRequest request) {
        contestNoticeService.createNotice(contestId, Long.parseLong(userId), request);
        return ResponseEntity.status(201).body(ApiResponse.created("공지사항이 등록되었습니다.", null));
    }

    // 9-3. 공지 수정
    @PatchMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> updateNotice(
            @PathVariable Long contestId,
            @PathVariable Long noticeId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody NoticeUpdateRequest request) {
        contestNoticeService.updateNotice(contestId, noticeId, Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 수정되었습니다.", null));
    }

    // 9-4. 공지 삭제
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @PathVariable Long contestId,
            @PathVariable Long noticeId,
            @AuthenticationPrincipal String userId) {
        contestNoticeService.deleteNotice(contestId, noticeId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("공지사항이 삭제되었습니다.", null));
    }
}
