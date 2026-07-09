package net.diveon.backend.global.exception;

public class InvitationCodeExpiredException extends RuntimeException {

    public InvitationCodeExpiredException() {
        super("초대링크가 만료되었습니다.");
    }
}
