package net.diveon.backend.global.exception;

public class SubmissionAccessDeniedException extends RuntimeException {
    public SubmissionAccessDeniedException(){
        super("제출물에 접근이 제한되었습니다.");
    }

    public SubmissionAccessDeniedException(String message) {
        super(message);
    }
}
