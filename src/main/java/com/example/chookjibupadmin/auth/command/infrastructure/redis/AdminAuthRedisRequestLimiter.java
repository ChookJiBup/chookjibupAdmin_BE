package com.example.chookjibupadmin.auth.command.infrastructure.redis;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.application.port.AdminAuthRequestLimiter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 이메일 원문을 노출하지 않는 Redis 키로 인증 요청 제한을 처리한다.
 */
@Component
@RequiredArgsConstructor
public class AdminAuthRedisRequestLimiter implements AdminAuthRequestLimiter {

    private static final String KEY_PREFIX = "admin:auth-limit:";
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('EXISTS', KEYS[2]) == 1 then
                        return 0
                    end
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                        redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end
                    if count > tonumber(ARGV[2]) then
                        return 0
                    end
                    redis.call('PSETEX', KEYS[2], ARGV[3], '1')
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(
            String action,
            AdminEmail email,
            int requestLimit,
            Duration requestWindow,
            Duration resendCooldown
    ) {
        String subject = digest(email.getValue());
        String baseKey = KEY_PREFIX + action + ":" + subject;
        Long result = redisTemplate.execute(
                ACQUIRE_SCRIPT,
                List.of(baseKey + ":count", baseKey + ":cooldown"),
                Long.toString(requestWindow.toMillis()),
                Integer.toString(requestLimit),
                Long.toString(resendCooldown.toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    private String digest(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash auth rate limit subject.", exception);
        }
    }
}
