package net.diveon.backend.global.exception;

public class SocialAccountConflictException extends RuntimeException {

    public SocialAccountConflictException() {
        super("이미 다른 로그인 방식으로 가입된 이메일입니다.");
    }
}
