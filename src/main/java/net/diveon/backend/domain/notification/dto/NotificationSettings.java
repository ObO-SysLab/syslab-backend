package net.diveon.backend.domain.notification.dto;

// 알림 설정. 행이 없으면 기본값(풀이 성공 켬, 댓글 켬, 대회 시작 끔).
public class NotificationSettings {

    private Boolean solveCorrect;
    private Boolean comment;
    private Boolean contestStart;

    public NotificationSettings() {
    }

    // null은 '바꾸지 않음'(설정 저장 요청에서 보낸 값만 바꿀 때)
    public NotificationSettings(Boolean solveCorrect, Boolean comment, Boolean contestStart) {
        this.solveCorrect = solveCorrect;
        this.comment = comment;
        this.contestStart = contestStart;
    }

    public static NotificationSettings defaults() {
        return new NotificationSettings(true, true, false);
    }

    public Boolean getSolveCorrect() { return solveCorrect; }
    public Boolean getComment() { return comment; }
    public Boolean getContestStart() { return contestStart; }
}
