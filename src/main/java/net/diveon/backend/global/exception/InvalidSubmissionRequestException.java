package net.diveon.backend.global.exception;

public class InvalidSubmissionRequestException extends RuntimeException {
    public InvalidSubmissionRequestException(String message) {
        super(message);
    }
}
