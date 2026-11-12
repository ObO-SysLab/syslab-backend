package net.diveon.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class GradingConfig {
    // 코딩 채점 발송 전용 스레드 하나. 주기 발송과 커밋 직후 발송이 차례로 돌고, 다른 예약 작업에 밀리지 않는다.
    @Bean("gradingScheduler")
    public ThreadPoolTaskScheduler gradingScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("grading-");
        return scheduler;
    }
}
