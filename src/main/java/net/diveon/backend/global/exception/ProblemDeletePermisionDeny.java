package net.diveon.backend.global.exception;

public class ProblemDeletePermisionDeny extends RuntimeException {

    public ProblemDeletePermisionDeny() {
        super("문제의 소유주만 삭제가 가능합니다");
    }
}
