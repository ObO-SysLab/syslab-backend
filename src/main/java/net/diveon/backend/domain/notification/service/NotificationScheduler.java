package net.diveon.backend.domain.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 정답 알림(20초)·대회 시작 알림(60초). 실패해도 다음 주기에 같은 위치부터 다시 처리한다.
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    static final int SOLVE_BATCH = 500;

    private final NotificationService notificationService;

    public NotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${notification.solve.poll-ms:20000}", initialDelayString = "${notification.solve.initial-delay-ms:30000}")
    public void solveResults() {
        try {
            notificationService.processSolveResults(SOLVE_BATCH);
        } catch (RuntimeException e) {
            log.warn("정답 알림 처리 실패: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${notification.contest.poll-ms:60000}", initialDelayString = "${notification.contest.initial-delay-ms:45000}")
    public void contestStarts() {
        try {
            notificationService.notifyContestStarts();
        } catch (RuntimeException e) {
            log.warn("대회 시작 알림 처리 실패: {}", e.getMessage());
        }
    }
}
