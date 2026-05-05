package net.diveon.backend.global.exception;

public class SubmissionNotCompletedException extends RuntimeException {
    public SubmissionNotCompletedException(String message) {
        super(message);
    }
}
