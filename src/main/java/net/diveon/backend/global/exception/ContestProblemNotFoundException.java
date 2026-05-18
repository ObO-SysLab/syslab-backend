package net.diveon.backend.global.exception;

public class ContestProblemNotFoundException extends RuntimeException {
    public ContestProblemNotFoundException() {
        super("존재하지 않는 대회 문제입니다.");
    }
}
