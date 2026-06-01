package net.diveon.backend.domain.user.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.user.dto.EmailSendRequest;
import net.diveon.backend.domain.user.dto.EmailVerifyRequest;
import net.diveon.backend.domain.user.dto.PasswordResetRequest;
import net.diveon.backend.domain.user.service.EmailService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email")
public class EmailVerifyController {

    private final EmailService emailService;

    public EmailVerifyController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@Valid @RequestBody EmailSendRequest request) {
        emailService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다.", null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody EmailVerifyRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다.", null));
    }

    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<Void>> findId(@Valid @RequestBody EmailSendRequest request) {
        emailService.sendLoginId(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("이메일로 아이디를 발송했습니다.", null));
    }

    @PostMapping("/send-reset")
    public ResponseEntity<ApiResponse<Void>> sendReset(@Valid @RequestBody EmailSendRequest request) {
        emailService.sendPasswordResetCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("비밀번호 재설정 인증 코드를 발송했습니다.", null));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        emailService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }
}
