package net.diveon.backend.global.exception;

public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException() {
        super("존재하지 않는 그룹입니다.");
    }
}
