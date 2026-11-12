package net.diveon.backend.domain.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 1분마다 점수·티어를 풀이 기록에 맞춘다(외부 채점기로 채점한 코딩 문제 포함). 처음 실행이 지난 기록까지 바로잡는다.
@Component
public class UserTierScheduler {

    private static final Logger log = LoggerFactory.getLogger(UserTierScheduler.class);

    private final UserTierService userTierService;

    public UserTierScheduler(UserTierService userTierService) {
        this.userTierService = userTierService;
    }

    @Scheduled(fixedDelayString = "${tier.refresh-ms:60000}", initialDelayString = "${tier.refresh-initial-delay-ms:20000}")
    public void refresh() {
        try {
            int changed = userTierService.refreshAllTierScores();
            if (changed > 0) log.info("점수·티어를 다시 맞춘 사용자 {}명", changed);
        } catch (RuntimeException e) {
            log.warn("점수·티어 갱신 실패: {}", e.getMessage());
        }
    }
}
