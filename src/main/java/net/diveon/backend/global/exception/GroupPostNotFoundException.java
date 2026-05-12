package net.diveon.backend.global.exception;

public class GroupPostNotFoundException extends RuntimeException {
    public GroupPostNotFoundException() {
        super("존재하지 않는 게시글입니다.");
    }

    public GroupPostNotFoundException(String message) {
        super(message);
    }
}
