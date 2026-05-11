package net.diveon.backend.global.exception;

public class GroupLeaderPermissionDeniedException extends RuntimeException {

    public GroupLeaderPermissionDeniedException() {
        super("해당 그룹의 그룹장 권한이 없습니다.");
    }
}
