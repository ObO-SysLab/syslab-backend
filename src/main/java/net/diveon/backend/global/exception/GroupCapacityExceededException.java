package net.diveon.backend.global.exception;

public class GroupCapacityExceededException extends RuntimeException {

    public GroupCapacityExceededException() {
        super("그룹 최대 인원수를 초과할 수 없습니다.");
    }
}
