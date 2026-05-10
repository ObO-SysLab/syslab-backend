package net.diveon.backend.global.exception;

public class GroupAssignRequestNotPendingException extends RuntimeException {

    public GroupAssignRequestNotPendingException() {
        super("Only pending group assign request can be canceled");
    }
}
