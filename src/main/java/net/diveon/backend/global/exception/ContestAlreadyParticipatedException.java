package net.diveon.backend.global.exception;

public class ContestAlreadyParticipatedException extends RuntimeException {
    public ContestAlreadyParticipatedException() {
        super("이미 참가한 대회입니다.");
    }
}
