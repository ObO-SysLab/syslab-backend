package net.diveon.backend.global.exception;

public class VmCreationFailedException extends RuntimeException {
    public VmCreationFailedException(String message) {
        super(message);
    }
}
