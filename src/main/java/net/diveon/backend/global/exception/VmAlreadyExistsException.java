package net.diveon.backend.global.exception;

public class VmAlreadyExistsException extends RuntimeException {
    public VmAlreadyExistsException(String message) {
        super(message);
    }
}