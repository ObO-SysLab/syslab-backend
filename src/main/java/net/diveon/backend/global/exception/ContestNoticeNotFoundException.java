package net.diveon.backend.global.exception;

public class ContestNoticeNotFoundException extends RuntimeException {
    public ContestNoticeNotFoundException() {
        super("공지사항을 찾을 수 없습니다.");
    }
}
