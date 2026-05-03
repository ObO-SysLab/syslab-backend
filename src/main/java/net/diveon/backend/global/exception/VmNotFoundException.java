package net.diveon.backend.global.exception;

public class VmNotFoundException extends RuntimeException {
    public VmNotFoundException(String message) {
        super(message);
    }
}