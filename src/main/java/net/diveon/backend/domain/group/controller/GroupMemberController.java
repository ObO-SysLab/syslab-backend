package net.diveon.backend.domain.group.controller;

import net.diveon.backend.domain.group.dto.GroupMemberCommonResponse;
import net.diveon.backend.domain.group.service.GroupMemberService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
