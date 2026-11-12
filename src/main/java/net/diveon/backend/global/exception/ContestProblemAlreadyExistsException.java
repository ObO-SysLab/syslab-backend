package net.diveon.backend.global.exception;

public class ContestProblemAlreadyExistsException extends RuntimeException {
    public ContestProblemAlreadyExistsException() {
        super("이미 대회에 추가된 문제입니다.");
    }
}
