package net.diveon.backend.global.exception;

public class GroupProblemAlreadyExistsException extends RuntimeException {
    public GroupProblemAlreadyExistsException() {
        super("이미 그룹에 추가된 문제입니다.");
    }
}
