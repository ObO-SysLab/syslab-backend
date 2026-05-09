package net.diveon.backend.global.exception;

public class SubmissionAccessDeniedException extends RuntimeException {
    public SubmissionAccessDeniedException(String message) {
        super(message);
    }
}
