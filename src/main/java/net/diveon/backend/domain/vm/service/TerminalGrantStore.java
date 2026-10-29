package net.diveon.backend.domain.vm.service;

import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TerminalGrantStore {
    private final RedisTemplate<String, String> redis;

    public TerminalGrantStore(RedisTemplate<String, String> redis) { this.redis = redis; }

    public void put(String kind, String token, String value, Duration ttl) {
        redis.opsForValue().set(key(kind, token), value, ttl);
    }

    public String consume(String token) {
        // Redis GETDEL is atomic across API instances (Redis >= 6.2).
        return redis.opsForValue().getAndDelete(key("ticket", token));
    }

    public String lease(String token) { return redis.opsForValue().get(key("lease", token)); }

    private String key(String kind, String token) { return "terminal:v1:" + kind + ":" + token; }
}
