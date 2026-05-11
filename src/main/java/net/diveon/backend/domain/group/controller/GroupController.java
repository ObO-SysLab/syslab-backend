package net.diveon.backend.domain.group.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.group.dto.GroupAddProblemsRequest;
import net.diveon.backend.domain.group.dto.GroupCreateRequest;
import net.diveon.backend.domain.group.dto.GroupCreateResponse;
import net.diveon.backend.domain.group.dto.GroupDetailResponse;
import net.diveon.backend.domain.group.dto.GroupListResponse;
import net.diveon.backend.domain.group.dto.GroupMyListResponse;
import net.diveon.backend.domain.group.dto.GroupProblemListResponse;
import org.springframework.web.bind.annotation.RequestParam;
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

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // 그룹 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<GroupListResponse>> searchGroups(
            @AuthenticationPrincipal String userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        Long parsedUserId = (userId != null && !userId.equals("anonymousUser")) ? Long.parseLong(userId) : null;
        GroupListResponse response = groupService.searchGroups(parsedUserId, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("그룹 검색에 성공하였습니다.", response));
    }

    // 그룹 목록 조회
    @GetMapping("")
    public ResponseEntity<ApiResponse<GroupListResponse>> getGroupList(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean isJoined) {
        Long parsedUserId = (userId != null && !userId.equals("anonymousUser")) ? Long.parseLong(userId) : null;
        GroupListResponse response = groupService.getGroupList(parsedUserId, page, size, tag, isJoined);
        return ResponseEntity.ok(ApiResponse.success("그룹 목록 조회에 성공하였습니다.", response));
    }

    // 내가 속한 그룹 목록 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<GroupMyListResponse>>> getMyGroups(
            @AuthenticationPrincipal String userId) {
        List<GroupMyListResponse> response = groupService.getMyGroups(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("내 그룹 목록 조회 성공", response));
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

    // 그룹 문제 목록 조회
    @GetMapping("/{groupId}/problems")
    public ResponseEntity<ApiResponse<GroupProblemListResponse>> getGroupProblems(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page) {
        GroupProblemListResponse response = groupService.getGroupProblems(groupId, Long.parseLong(userId), page);
        return ResponseEntity.ok(ApiResponse.success("그룹 문제 목록 조회 성공", response));
    }

    // 공개 문제 그룹에 추가
    @PostMapping("/{groupId}/problems")
    public ResponseEntity<ApiResponse<Void>> addProblems(
            @PathVariable Long groupId,
            @AuthenticationPrincipal String userId,
            @RequestBody GroupAddProblemsRequest request) {
        groupService.addProblems(groupId, Long.parseLong(userId), request);
        return ResponseEntity.status(201).body(ApiResponse.created("문제가 그룹에 추가되었습니다.", null));
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
