package net.diveon.backend.global.exception;

import org.springframework.http.HttpStatus;

// 상점·알림·탈퇴의 규칙 위반을 상태 코드와 사용자에게 보여 줄 문구로 돌려준다.
public class DomainRuleException extends RuntimeException {

    private final HttpStatus status;

    public DomainRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
