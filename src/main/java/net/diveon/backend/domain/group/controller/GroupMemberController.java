package net.diveon.backend.domain.group.controller;

import net.diveon.backend.domain.group.dto.GroupMemberCommonResponse;
import net.diveon.backend.domain.group.dto.GroupMemberKickResponse;
import net.diveon.backend.domain.group.dto.GroupPendingMemberListResponse;
import net.diveon.backend.domain.group.service.GroupMemberService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/members")
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    public GroupMemberController(GroupMemberService groupMemberService) {
        this.groupMemberService = groupMemberService;
    }

    // 그룹 가입 신청
    @PostMapping
    public ResponseEntity<ApiResponse<GroupMemberCommonResponse>> applyGroupMembership(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId) {
        GroupMemberCommonResponse response = groupMemberService.applyGroupMembership(groupId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("가입 신청이 완료되었습니다.", response));
    }

    // 그룹 가입 대기자 목록 조회
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<GroupPendingMemberListResponse>> getPendingMembers(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        GroupPendingMemberListResponse response = groupMemberService.getPendingMembers(
                groupId,
                Long.parseLong(userId),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("가입 대기자 목록 조회 성공", response));
    }

    // 그룹 가입 신청 철회
    @PatchMapping("/pending/me")
    public ResponseEntity<ApiResponse<GroupMemberCommonResponse>> cancelPendingGroupMembership(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId) {
        GroupMemberCommonResponse response = groupMemberService.cancelPendingGroupMembership(
                groupId,
                Long.parseLong(userId)
        );
        return ResponseEntity.ok(ApiResponse.success("성공적으로 철회 되었습니다.", response));
    }

    // 그룹 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<GroupMemberCommonResponse>> leaveGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId) {
        GroupMemberCommonResponse response = groupMemberService.leaveGroup(groupId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("성공적으로 탈퇴(또는 신청 철회) 되었습니다.", response));
    }

    // 그룹 멤버 강제 퇴장
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<GroupMemberKickResponse>> kickGroupMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @AuthenticationPrincipal String requesterId) {
        GroupMemberKickResponse response = groupMemberService.kickGroupMember(
                groupId,
                Long.parseLong(requesterId),
                userId
        );
        return ResponseEntity.ok(ApiResponse.success("해당 멤버를 성공적으로 강퇴했습니다.", response));
    }
}
