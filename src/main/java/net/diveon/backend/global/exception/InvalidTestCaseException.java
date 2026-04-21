package net.diveon.backend.global.exception;

public class InvalidTestCaseException extends RuntimeException {
    public InvalidTestCaseException() {
        super("유효하지 않은 테스트케이스입니다.");
    }

    public InvalidTestCaseException(String message) {
        super(message);
    }
}
