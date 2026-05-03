package net.diveon.backend.global.exception;

public class VmAccessDeniedException extends RuntimeException {
    public VmAccessDeniedException(String message) {
        super(message);
    }
}