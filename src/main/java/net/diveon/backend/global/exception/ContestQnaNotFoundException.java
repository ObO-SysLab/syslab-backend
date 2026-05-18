package net.diveon.backend.global.exception;

public class ContestQnaNotFoundException extends RuntimeException {
    public ContestQnaNotFoundException() {
        super("존재하지 않는 Q&A입니다.");
    }
}
