package net.diveon.backend.global.exception;

public class SubmissionAccessDeniedException extends RuntimeException {
    //이거 바로 아래는 컴파일 뻑나는거 방지용, 나중에 알아서 수정해
    public SubmissionAccessDeniedException(){
        super("제출물에 접근이 제한되었습니다.");
    }

    public SubmissionAccessDeniedException(String message) {
        super(message);
    }
}
