package net.diveon.backend.global.config;

import net.diveon.backend.domain.vm.service.TerminalGrantStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RedisWiringTest {
    @Test
    void terminalGrantsUseConfiguredTemplateWhenBootAlsoProvidesStringTemplate() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withUserConfiguration(RedisConfig.class, TerminalGrantStore.class)
                .withPropertyValues("spring.data.redis.host=127.0.0.1", "spring.data.redis.port=9")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(RedisTemplate.class))
                            .containsOnlyKeys("redisTemplate", "stringRedisTemplate");
                    assertThat(ReflectionTestUtils.getField(context.getBean(TerminalGrantStore.class), "redis"))
                            .isSameAs(context.getBean("redisTemplate"));
                });
    }
}
