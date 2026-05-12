package net.diveon.backend.global.exception;

public class GroupUserNotFoundException extends RuntimeException {

    public GroupUserNotFoundException() {
        super("존재하지 않는 그룹 유저입니다.");
    }
}
