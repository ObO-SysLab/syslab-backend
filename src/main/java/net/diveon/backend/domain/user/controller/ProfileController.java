package net.diveon.backend.domain.user.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.problem.dto.response.ProblemListResponse;
import net.diveon.backend.domain.user.dto.MyContestListResponse;
import net.diveon.backend.domain.user.dto.MyGroupListResponse;
import net.diveon.backend.domain.user.dto.PasswordUpdateRequest;
import net.diveon.backend.domain.user.dto.ProfileShowResponse;
import net.diveon.backend.domain.user.dto.ProfileUpdateRequest;
import net.diveon.backend.domain.user.dto.UserCategoryStatsResponse;
import net.diveon.backend.domain.user.service.MyPageContestService;
import net.diveon.backend.domain.user.service.MyPageGroupService;
import net.diveon.backend.domain.user.service.MyPageProblemService;
import net.diveon.backend.domain.user.service.ProfileService;
import net.diveon.backend.domain.user.service.UserCategoryStatsService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final UserCategoryStatsService userCategoryStatsService;
    private final MyPageProblemService myPageProblemService;
    private final MyPageGroupService myPageGroupService;
    private final MyPageContestService myPageContestService;

    public ProfileController(ProfileService profileService,
                             UserCategoryStatsService userCategoryStatsService,
                             MyPageProblemService myPageProblemService,
                             MyPageGroupService myPageGroupService,
                             MyPageContestService myPageContestService) {
        this.profileService = profileService;
        this.userCategoryStatsService = userCategoryStatsService;
        this.myPageProblemService = myPageProblemService;
        this.myPageGroupService = myPageGroupService;
        this.myPageContestService = myPageContestService;
    }
    // 프로필 조회
    @GetMapping("/show")
    public ResponseEntity<ApiResponse<ProfileShowResponse>> show(@AuthenticationPrincipal String userId) {
        ProfileShowResponse response = profileService.getProfile(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("성공", response));
    }

    // 프로필 수정
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody ProfileUpdateRequest request) {
        profileService.updateProfile(Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", null));
    }

    // 카테고리별 점수 (거미줄 차트)
    @GetMapping("/category-stats")
    public ResponseEntity<ApiResponse<UserCategoryStatsResponse>> getCategoryStats(
            @AuthenticationPrincipal String userId) {
        UserCategoryStatsResponse response = userCategoryStatsService.getCategoryStats(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("카테고리별 점수 조회에 성공하였습니다.", response));
    }

    // 푼 문제 목록 (visibility 필터: public/private/group/contest, 미입력 시 전체)
    @GetMapping("/problems/solved")
    public ResponseEntity<ApiResponse<ProblemListResponse>> getSolvedProblems(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String visibility) {
        return ResponseEntity.ok(ApiResponse.success("푼 문제 목록 조회에 성공하였습니다.",
                myPageProblemService.getSolvedProblems(Long.parseLong(userId), page, visibility)));
    }

    // 못 푼 문제 목록 (WRONG 기록 있고 CORRECT 없는 문제, visibility 필터 가능)
    @GetMapping("/problems/failed")
    public ResponseEntity<ApiResponse<ProblemListResponse>> getFailedProblems(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String visibility) {
        return ResponseEntity.ok(ApiResponse.success("못 푼 문제 목록 조회에 성공하였습니다.",
                myPageProblemService.getFailedProblems(Long.parseLong(userId), page, visibility)));
    }

    // 출제한 문제 목록 (visibility 필터 가능)
    @GetMapping("/problems/authored")
    public ResponseEntity<ApiResponse<ProblemListResponse>> getAuthoredProblems(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String visibility) {
        return ResponseEntity.ok(ApiResponse.success("출제한 문제 목록 조회에 성공하였습니다.",
                myPageProblemService.getAuthoredProblems(Long.parseLong(userId), page, visibility)));
    }

    // 속한 그룹 목록
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<MyGroupListResponse>> getMyGroups(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.success("속한 그룹 목록 조회에 성공하였습니다.",
                myPageGroupService.getMyGroups(Long.parseLong(userId), page)));
    }

    // 속한 대회 목록 (종료된 대회 포함)
    @GetMapping("/contests")
    public ResponseEntity<ApiResponse<MyContestListResponse>> getMyContests(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.success("속한 대회 목록 조회에 성공하였습니다.",
                myPageContestService.getMyContests(Long.parseLong(userId), page)));
    }

    // 비밀번호 변경

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PasswordUpdateRequest request) {
        profileService.updatePassword(Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }
}
