package net.diveon.backend.global.exception;

public class ContestAlreadyStartedException extends RuntimeException {
    public ContestAlreadyStartedException() {
        super("이미 시작된 대회는 참가 신청 또는 취소가 불가능합니다.");
    }
}
