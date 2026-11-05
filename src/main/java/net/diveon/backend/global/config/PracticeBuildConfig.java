package net.diveon.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class PracticeBuildConfig {
    // Keep unrelated scheduled jobs on their previous single-thread execution model.
    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("scheduling-");
        return scheduler;
    }

    @Bean("practiceBuildClock")
    public Clock practiceBuildClock() { return Clock.systemUTC(); }

    @Bean("practiceBuildScheduler")
    public ThreadPoolTaskScheduler practiceBuildScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("practice-build-");
        return scheduler;
    }
}
