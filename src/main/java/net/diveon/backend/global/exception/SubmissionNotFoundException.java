package net.diveon.backend.global.exception;

public class SubmissionNotFoundException extends RuntimeException {
    public SubmissionNotFoundException() {
        super("제출물이 없습니다.");
    }

    public SubmissionNotFoundException(String message) {
        super(message);
    }
}
