package net.diveon.backend.global.exception;

public class GoogleOAuthException extends RuntimeException {

    public GoogleOAuthException() {
        super("구글 로그인 처리에 실패했습니다.");
    }

    public GoogleOAuthException(String message) {
        super(message);
    }
}
