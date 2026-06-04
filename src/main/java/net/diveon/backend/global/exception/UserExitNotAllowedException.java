package net.diveon.backend.global.exception;

public class UserExitNotAllowedException extends RuntimeException {

    public UserExitNotAllowedException(String message) {
        super(message);
    }
}
