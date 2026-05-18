package net.diveon.backend.global.exception;

public class ContestNotFoundException extends RuntimeException {
    public ContestNotFoundException() {
        super("존재하지 않는 대회입니다.");
    }
}
