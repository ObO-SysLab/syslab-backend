package net.diveon.backend.global.exception;

public class SubmissionNotFoundException extends RuntimeException {
    // 이 아래는 컴파일 뻑 방지용, 알아서 제거할 것
    public SubmissionNotFoundException() {
        super("제출물이 없습니다.");
    }

    public SubmissionNotFoundException(String message) {
        super(message);
    }
}
