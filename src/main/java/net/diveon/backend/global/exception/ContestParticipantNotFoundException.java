package net.diveon.backend.global.exception;

public class ContestParticipantNotFoundException extends RuntimeException {
    public ContestParticipantNotFoundException() {
        super("대회 참가자를 찾을 수 없습니다.");
    }

    public ContestParticipantNotFoundException(String message) {
        super(message);
    }
}
