package net.diveon.backend.global.exception;

public class ContestQnaAnswerNotFoundException extends RuntimeException {
    public ContestQnaAnswerNotFoundException() {
        super("존재하지 않는 답변입니다.");
    }
}
