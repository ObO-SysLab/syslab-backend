package net.diveon.backend.domain.group.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.group.dto.GroupCreateRequest;
import net.diveon.backend.domain.group.dto.GroupCreateResponse;
import net.diveon.backend.domain.group.service.GroupService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public ResponseEntity<ApiResponse<GroupCreateResponse>> createGroup(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody GroupCreateRequest request) {
        GroupCreateResponse response = groupService.createGroup(Long.parseLong(userId), request);
        return ResponseEntity.status(201).body(ApiResponse.created("그룹이 성공적으로 생성되었습니다.", response));
    }
}
