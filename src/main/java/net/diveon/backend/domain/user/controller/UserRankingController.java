package net.diveon.backend.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.user.dto.UserRankingResponse;
import net.diveon.backend.domain.user.service.UserRankingService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/users")
public class UserRankingController {

    private final UserRankingService userRankingService;

    public UserRankingController(UserRankingService userRankingService) {
        this.userRankingService = userRankingService;
    }

    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<UserRankingResponse>> getUserRanking(
        @AuthenticationPrincipal String userId,
        @RequestParam(defaultValue = "1") int page
    ) {
        UserRankingResponse response = userRankingService.getRanking(Long.parseLong(userId), page);
        return ResponseEntity.ok(ApiResponse.success("유저 랭킹 조회에 성공하였습니다.", response));
    }
}
