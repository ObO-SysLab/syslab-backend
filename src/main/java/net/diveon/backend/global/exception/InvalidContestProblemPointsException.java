package net.diveon.backend.global.exception;

public class InvalidContestProblemPointsException extends RuntimeException {

    public InvalidContestProblemPointsException() {
        super("문제 배점은 0점 이상이어야 합니다.");
    }
}
