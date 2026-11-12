package net.diveon.backend.domain.user.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.user.service.UserWithdrawService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/users/me")
public class UserAccountController {

    private final UserWithdrawService withdrawService;

    public UserAccountController(UserWithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    // 계정 탈퇴. 본문 {"nickname": "현재 닉네임"}으로 본인 확인(구글 가입자는 비밀번호가 없어 닉네임으로 확인).
    // 본문이 있는 DELETE는 중간 프록시에서 빠질 수 있어 POST로 받는다.
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal String userId,
                                                      @RequestBody Map<String, String> body) {
        withdrawService.withdraw(Long.parseLong(userId), body == null ? null : body.get("nickname"));
        return ResponseEntity.ok(ApiResponse.success("탈퇴했습니다.", null));
    }
}
