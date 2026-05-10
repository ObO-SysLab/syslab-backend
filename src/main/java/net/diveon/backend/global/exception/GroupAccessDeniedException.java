package net.diveon.backend.global.exception;

public class GroupAccessDeniedException extends RuntimeException {
    public GroupAccessDeniedException() {
        super("그룹 멤버가 아닙니다.");
    }
}
