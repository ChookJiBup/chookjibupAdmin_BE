package com.example.chookjibupadmin.auth.command.infrastructure.redis;

import com.example.chookjibupadmin.auth.command.domain.AdminPasswordResetTokenRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * 해시된 비밀번호 재설정 토큰을 Redis TTL과 함께 저장한다.
 */
@Repository
@RequiredArgsConstructor
public class AdminPasswordResetTokenRedisRepository
        implements AdminPasswordResetTokenRepository {

    private static final String TOKEN_KEY_PREFIX = "admin:password-reset:token:";
    private static final String ACCOUNT_KEY_PREFIX = "admin:password-reset:account:";
    private static final DefaultRedisScript<Long> DELETE_IF_MATCHED_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long adminAccountId, String tokenHash, Duration ttl) {
        String accountKey = accountKey(adminAccountId);
        String previousTokenHash = redisTemplate.opsForValue().get(accountKey);
        if (previousTokenHash != null) {
            redisTemplate.delete(tokenKey(previousTokenHash));
        }

        redisTemplate.opsForValue().set(
                tokenKey(tokenHash),
                adminAccountId.toString(),
                ttl
        );
        redisTemplate.opsForValue().set(accountKey, tokenHash, ttl);
    }

    @Override
    public Optional<Long> consume(String tokenHash) {
        String adminAccountId = redisTemplate.opsForValue()
                .getAndDelete(tokenKey(tokenHash));
        if (adminAccountId == null) {
            return Optional.empty();
        }

        Long id = Long.valueOf(adminAccountId);
        deleteAccountKeyIfMatched(id, tokenHash);
        return Optional.of(id);
    }

    @Override
    public void delete(Long adminAccountId, String tokenHash) {
        redisTemplate.delete(tokenKey(tokenHash));
        deleteAccountKeyIfMatched(adminAccountId, tokenHash);
    }

    private void deleteAccountKeyIfMatched(Long adminAccountId, String tokenHash) {
        redisTemplate.execute(
                DELETE_IF_MATCHED_SCRIPT,
                List.of(accountKey(adminAccountId)),
                tokenHash
        );
    }

    private String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private String accountKey(Long adminAccountId) {
        return ACCOUNT_KEY_PREFIX + adminAccountId;
    }
}
