package net.diveon.backend.global.exception;

public class ProblemNotFoundException extends RuntimeException {
    public ProblemNotFoundException() {
        super("문제를 찾을 수 없습니다.");
    }

    public ProblemNotFoundException(String message) {
        super(message);
    }
}
