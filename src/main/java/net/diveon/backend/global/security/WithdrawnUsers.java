package net.diveon.backend.global.security;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// 탈퇴한 사용자의 남은 액세스 토큰을 막는다. 액세스 토큰 유효시간 동안만 Redis 키를 둔다.
// Redis 오류 때 모든 요청이 막히지 않도록 확인은 실패하면 통과시킨다(리프레시 토큰은 이미 지워져 곧 만료된다).
public class WithdrawnUsers {

    static final String PREFIX = "withdrawn_user:";
    private static final Logger log = LoggerFactory.getLogger(WithdrawnUsers.class);

    private final StringRedisTemplate redis;

    public WithdrawnUsers(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void mark(long userId, Duration ttl) {
        redis.opsForValue().set(PREFIX + userId, "1", ttl);
    }

    public boolean isWithdrawn(String userId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + userId));
        } catch (RuntimeException e) {
            log.warn("탈퇴 사용자 확인 실패(통과 처리): {}", e.getMessage());
            return false;
        }
    }
}
