package net.diveon.backend.global.exception;

public class ContestCooldownException extends RuntimeException {
    public ContestCooldownException() {
        super("제출 쿨다운 중입니다. 30초 후 다시 시도해주세요.");
    }
}
