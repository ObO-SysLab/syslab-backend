package net.diveon.backend.global.exception;

public class ContestNotOngoingException extends RuntimeException {
    public ContestNotOngoingException() {
        super("진행 중인 대회가 아닙니다.");
    }
}
