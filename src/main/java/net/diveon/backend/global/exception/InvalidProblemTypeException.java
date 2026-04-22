package net.diveon.backend.global.exception;

public class InvalidProblemTypeException extends RuntimeException {
    public InvalidProblemTypeException() {
        super("유효하지 않은 문제 유형입니다.");
    }

    public InvalidProblemTypeException(String message) {
        super(message);
    }
}
