package com.example.chookjibupadmin.global.security.internal;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis TTL로 internal API nonce 재사용을 차단한다.
 */
@Repository
@RequiredArgsConstructor
public class InternalApiRedisNonceRepository
        implements InternalApiNonceRepository {

    private static final String KEY_PREFIX = "internal-auth:nonce:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean saveIfAbsent(
            String clientId,
            String nonce,
            Duration ttl
    ) {
        Boolean saved = redisTemplate.opsForValue().setIfAbsent(
                key(clientId, nonce),
                "1",
                ttl
        );

        return Boolean.TRUE.equals(saved);
    }

    private String key(
            String clientId,
            String nonce
    ) {
        return KEY_PREFIX + clientId + ":" + nonce;
    }
}
