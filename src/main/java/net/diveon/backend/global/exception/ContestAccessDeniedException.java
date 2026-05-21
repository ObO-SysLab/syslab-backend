package net.diveon.backend.global.exception;

public class ContestAccessDeniedException extends RuntimeException {
    public ContestAccessDeniedException() {
        super("대회에 대한 권한이 없습니다.");
    }

    public ContestAccessDeniedException(String message){
        super(message);
    }
}
