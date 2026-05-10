package net.diveon.backend.domain.group.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.group.dto.GroupCreateRequest;
import net.diveon.backend.domain.group.dto.GroupCreateResponse;
import net.diveon.backend.domain.group.dto.GroupDetailResponse;
import net.diveon.backend.domain.group.service.GroupService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // 그룹 상세 조회
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDetailResponse>> getGroupDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId) {
        Long parsedUserId = (userId != null && !userId.equals("anonymousUser")) ? Long.parseLong(userId) : null;
        GroupDetailResponse response = groupService.getGroupDetail(groupId, parsedUserId);
        return ResponseEntity.ok(ApiResponse.success("그룹 상세 정보 조회 성공", response));
    }

    // 그룹 생성
    @PostMapping
    public ResponseEntity<ApiResponse<GroupCreateResponse>> createGroup(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody GroupCreateRequest request) {
        GroupCreateResponse response = groupService.createGroup(Long.parseLong(userId), request);
        return ResponseEntity.status(201).body(ApiResponse.created("그룹이 성공적으로 생성되었습니다.", response));
    }
}
