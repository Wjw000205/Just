package org.example.just.service.oa;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisOaAuthStateStore implements OaAuthStateStore {

    private static final String PREFIX = "oa:auth:state:";
    private static final String SEPARATOR = "\n";

    private final StringRedisTemplate redisTemplate;
    private final OaAuthProperties properties;

    @Override
    public void save(String state, OaAuthState authState) {
        String value = authState.codeVerifier() + SEPARATOR + authState.redirectUri();
        long ttl = Math.max(1, properties.getStateTtlSeconds());
        redisTemplate.opsForValue().set(key(state), value, Duration.ofSeconds(ttl));
    }

    @Override
    public OaAuthState consume(String state) {
        if (!StringUtils.hasText(state)) {
            return null;
        }
        String key = key(state);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        redisTemplate.delete(key);
        int splitIndex = value.indexOf(SEPARATOR);
        if (splitIndex <= 0 || splitIndex >= value.length() - 1) {
            return null;
        }
        return new OaAuthState(value.substring(0, splitIndex), value.substring(splitIndex + 1));
    }

    private String key(String state) {
        return PREFIX + state;
    }
}
