package net.diveon.backend.global.exception;

public class ProblemAccessDeniedException extends RuntimeException {
    public ProblemAccessDeniedException() {
        super("본인이 생성한 문제만 수정/삭제할 수 있습니다.");
    }
}
