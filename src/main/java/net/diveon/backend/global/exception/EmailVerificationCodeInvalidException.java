package net.diveon.backend.global.exception;

public class EmailVerificationCodeInvalidException extends RuntimeException {
    public EmailVerificationCodeInvalidException() {
        super("인증 코드가 올바르지 않거나 만료되었습니다.");
    }
}
