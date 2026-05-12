package net.diveon.backend.global.exception;

public class GroupLeaderCannotLeaveException extends RuntimeException {

    public GroupLeaderCannotLeaveException() {
        super("그룹장은 탈퇴가 불가능합니다");
    }
}
